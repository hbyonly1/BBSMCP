package theblocklab.bbsmcp.mcp.tools.film;

import theblocklab.bbsmcp.mcp.tools.core.*;

/**
 * FilmManager 工具提供者
 */
public class FilmManagerMCPTools extends MCPToolProvider {
    public FilmManagerMCPTools() {
        registerTool(new CreateFilmTool());
        registerTool(new GetFilmListTool());
    }
}
