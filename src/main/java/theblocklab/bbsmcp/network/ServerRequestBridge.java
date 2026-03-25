package theblocklab.bbsmcp.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import theblocklab.bbsmcp.exception.BBSMCPError;
import theblocklab.bbsmcp.exception.BBSMCPException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 封装并管理向客户端异步请求并等待确认 (ACK/OK) 的桥接器
 */
public class ServerRequestBridge {

    // K: requestId, V: 等待的 Future
    private static final ConcurrentHashMap<Integer, CompletableFuture<Boolean>> PENDING_REQUESTS = new ConcurrentHashMap<>();
    
    // K: requestId, V: 等待返回泛型数据 (通常是 JSON String) 的 Future
    private static final ConcurrentHashMap<Integer, CompletableFuture<String>> PENDING_DATA_REQUESTS = new ConcurrentHashMap<>();
    
    private static final AtomicInteger REQUEST_COUNTER = new AtomicInteger(0);

    // 用于超时的调度器
    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(1);

    /**
     * 用于流式写入请求体数据的函数式接口
     */
    @FunctionalInterface
    public interface PayloadWriter {
        void write(PacketByteBuf buf);
    }

    /**
     * 初始化桥接器，在服务器启动/网络注册时调用
     */
    public static void setup() {
        // 注册全局 ACK (OK) 数据包监听
        ServerPlayNetworking.registerGlobalReceiver(ServerNetwork.OK,
                (server, player, handler, buf, responseSender) -> {
                    int requestId = buf.readInt();

                    // 从挂起队列中取出并移除对应的 Promise
                    CompletableFuture<Boolean> future = PENDING_REQUESTS.remove(requestId);
                    if (future != null) {
                        // 唤醒挂起的线程
                        future.complete(true);
                    }
                });

        // 注册全局泛型数据回传包监听
        ServerPlayNetworking.registerGlobalReceiver(ServerNetwork.CLIENT_DATA_RESPONSE,
                (server, player, handler, buf, responseSender) -> {
                    int requestId = buf.readInt();
                    String dataString = buf.readString();

                    CompletableFuture<String> future = PENDING_DATA_REQUESTS.remove(requestId);
                    if (future != null) {
                        future.complete(dataString);
                    }
                });

        // 注册全局 ERROR 回传数据包监听
        ServerPlayNetworking.registerGlobalReceiver(ServerNetwork.CLIENT_ERROR,
                (server, player, handler, buf, responseSender) -> {
                    int requestId = buf.readInt();
                    String errorMessage = buf.readString();

                    CompletableFuture<Boolean> future = PENDING_REQUESTS.remove(requestId);
                    if (future != null) {
                        // 不再当作 true 回传，而是直接强行中止整个异步流链条，抛出真正的内部异常！
                        future.completeExceptionally(new BBSMCPException(
                                BBSMCPError.CLIENT_EXECUTION_FAILED, errorMessage));
                    }

                    CompletableFuture<String> dataFuture = PENDING_DATA_REQUESTS.remove(requestId);
                    if (dataFuture != null) {
                        dataFuture.completeExceptionally(new BBSMCPException(
                                BBSMCPError.CLIENT_EXECUTION_FAILED, errorMessage));
                    }
                });
    }

    /**
     * 向客户端发起请求并返回一个将在收到确认后触发的 CompletableFuture
     *
     * @param player        目标玩家
     * @param packetId      数据包的 Identifier (如 CLIENT_OPEN_FILM_PANEL)
     * @param payloadWriter 写入请求业务数据的方法
     * @return 异步操作结点的 CompletableFuture
     */
    public static CompletableFuture<Boolean> request(ServerPlayerEntity player, Identifier packetId,
            PayloadWriter payloadWriter) {
        // 1. 生成唯一 ID
        int requestId = REQUEST_COUNTER.incrementAndGet();
        CompletableFuture<Boolean> promise = new CompletableFuture<>();

        // 2. 存入待处理 Map
        PENDING_REQUESTS.put(requestId, promise);

        try {
            // 3. 构建数据包，确保第一个发出的数据总是 requestId
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeInt(requestId);

            // 4. 调用业务方提供的方法，将业务负载追加到 requestId 后面
            if (payloadWriter != null) {
                payloadWriter.write(buf);
            }

            // 5. 统一发送协议包
            ServerPlayNetworking.send(player, packetId, buf);

            // 6. 追加超时控制 (防止客户端断线或BUG导致内存泄漏，默认 10 秒超时)
            SCHEDULER.schedule(() -> {
                CompletableFuture<Boolean> pending = PENDING_REQUESTS.remove(requestId);
                if (pending != null && !pending.isDone()) {
                    pending.completeExceptionally(
                            new java.util.concurrent.TimeoutException("等待客户端请求超时: " + packetId.toString()));
                }
            }, 10, TimeUnit.SECONDS);

        } catch (Exception e) {
            PENDING_REQUESTS.remove(requestId);
            promise.completeExceptionally(e);
        }

        return promise;
    }

    /**
     * 发起一个附带预期数据载荷返回的网桥请求
     */
    public static CompletableFuture<String> requestData(ServerPlayerEntity player, Identifier packetId,
            PayloadWriter payloadWriter) {
        int requestId = REQUEST_COUNTER.incrementAndGet();
        CompletableFuture<String> promise = new CompletableFuture<>();

        PENDING_DATA_REQUESTS.put(requestId, promise);

        try {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeInt(requestId);

            if (payloadWriter != null) {
                payloadWriter.write(buf);
            }

            ServerPlayNetworking.send(player, packetId, buf);

            SCHEDULER.schedule(() -> {
                CompletableFuture<String> pending = PENDING_DATA_REQUESTS.remove(requestId);
                if (pending != null && !pending.isDone()) {
                    pending.completeExceptionally(
                            new java.util.concurrent.TimeoutException("等待客户端拿取数据请求超时: " + packetId.toString()));
                }
            }, 10, TimeUnit.SECONDS);

        } catch (Exception e) {
            PENDING_DATA_REQUESTS.remove(requestId);
            promise.completeExceptionally(e);
        }

        return promise;
    }
}
