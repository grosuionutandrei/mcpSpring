package dk.timer.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "urls")
public record UrlProperties(String base, String projectsApi, String tasksApi, String organizationApi) {
}



