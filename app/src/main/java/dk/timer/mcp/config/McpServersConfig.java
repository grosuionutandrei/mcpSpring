package dk.timer.mcp.config;

import java.util.List;

import dk.timer.mcp.spi.OrganizationPrompts;
import dk.timer.mcp.spi.OrganizationTools;
import dk.timer.mcp.spi.ProjectPrompts;
import dk.timer.mcp.spi.ProjectTools;
import dk.timer.mcp.timer.TimerTools;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.mcp.annotation.provider.complete.SyncStatelessMcpCompleteProvider;
import org.springframework.ai.mcp.annotation.provider.prompt.SyncStatelessMcpPromptProvider;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.ai.mcp.annotation.provider.tool.SyncStatelessMcpToolProvider;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStatelessServerTransport;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * One Spring Boot app, several MCP servers:
 * <p>
 * POST /mcp/projects -> project and task lookup tools (Gant)
 * POST /mcp/timer    -> timer tools (TimerTools)
 * <p>
 * Each server = transport (owns the URL) + server (owns the tool list) +
 * a RouterFunction bean that plugs the transport into Spring MVC.
 * To a client these are completely separate MCP servers.
 * <p>
 * The auto-configured single server on /mcp is switched off with
 * spring.ai.mcp.server.enabled=false, so tools are only exposed here.
 */
@Configuration
public class McpServersConfig {

    private static McpSchema.ServerCapabilities toolsOnly() {
        return McpSchema.ServerCapabilities.builder().tools(false).build();
    }

    // ------------------------------------------------------------------ projects

    @Bean
    WebMvcStatelessServerTransport projectsTransport(JsonMapper jsonMapper) {
        return WebMvcStatelessServerTransport.builder()
                .jsonMapper(new JacksonMcpJsonMapper(jsonMapper))
                .messageEndpoint("/mcp/projects")
                .build();
    }

    @Bean(destroyMethod = "close")
    McpStatelessSyncServer projectsServer(
            @Qualifier("projectsTransport") WebMvcStatelessServerTransport transport,
            ProjectTools projectTools,
            ObjectProvider<ProjectPrompts> projectPromptsProvider) {
        List<Object> projectPrompts = projectPromptsProvider.stream().map(Object.class::cast).toList();
        return McpServer.sync(transport)
                .serverInfo("timer-projects", "1.0.0")
                .instructions("Look up the projects of an organization and the tasks of a project.")
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(false)
                        .prompts(false)
                        .completions()
                        .build())
                .tools(new SyncStatelessMcpToolProvider(List.of(projectTools)).getToolSpecifications())
                .prompts(new SyncStatelessMcpPromptProvider(projectPrompts).getPromptSpecifications())
                .completions(new SyncStatelessMcpCompleteProvider(projectPrompts).getCompleteSpecifications())
                .build();
    }

    @Bean
    RouterFunction<ServerResponse> projectsRoutes(
            @Qualifier("projectsTransport") WebMvcStatelessServerTransport transport) {
        return transport.getRouterFunction();
    }

    // --------------------------------------------------------------------- timer

    @Bean
    WebMvcStatelessServerTransport timerTransport(JsonMapper jsonMapper) {
        return WebMvcStatelessServerTransport.builder()
                .jsonMapper(new JacksonMcpJsonMapper(jsonMapper))
                .messageEndpoint("/mcp/timer")
                .build();
    }

    @Bean(destroyMethod = "close")
    McpStatelessSyncServer timerServer(
            @Qualifier("timerTransport") WebMvcStatelessServerTransport transport,
            TimerTools timerTools) {
        return McpServer.sync(transport)
                .serverInfo("timer-timers", "1.0.0")
                .instructions("Start and manage timers.")
                .capabilities(toolsOnly())
                .tools(new SyncStatelessMcpToolProvider(List.of(timerTools)).getToolSpecifications())
                .build();
    }

    @Bean
    RouterFunction<ServerResponse> timerRoutes(
            @Qualifier("timerTransport") WebMvcStatelessServerTransport transport) {
        return transport.getRouterFunction();
    }


    //==============================================organization

    @Bean
    WebMvcStatelessServerTransport organizationTransport(JsonMapper jsonMapper) {
        return WebMvcStatelessServerTransport.builder()
                .jsonMapper(new JacksonMcpJsonMapper(jsonMapper))
                .messageEndpoint("/mcp/organization")
                .build();
    }

    @Bean(destroyMethod = "close")
    McpStatelessSyncServer organizationServer(
            @Qualifier("organizationTransport") WebMvcStatelessServerTransport transport,
            OrganizationTools organizationTools,
            ObjectProvider<OrganizationPrompts> organizationPromptsProvider) {
        List<Object> projectPrompts = organizationPromptsProvider.stream().map(Object.class::cast).toList();
        return McpServer.sync(transport)
                .serverInfo("timer-organization", "1.0.0")
                .instructions("Look up if an organization exists, create, delete, update, organizations")
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(false)
                        .prompts(false)
                        .completions()
                        .build())
                .tools(new SyncStatelessMcpToolProvider(List.of(organizationTools)).getToolSpecifications())
                .prompts(new SyncStatelessMcpPromptProvider(projectPrompts).getPromptSpecifications())
                .completions(new SyncStatelessMcpCompleteProvider(projectPrompts).getCompleteSpecifications())
                .build();
    }

    @Bean
    RouterFunction<ServerResponse> organizationRoutes(
            @Qualifier("organizationTransport") WebMvcStatelessServerTransport transport) {
        return transport.getRouterFunction();
    }

}
