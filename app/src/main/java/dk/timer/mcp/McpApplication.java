package dk.timer.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = {"dk.timer.mcp", "com.example"})
@ConfigurationPropertiesScan
public class McpApplication {

	static void main(String[] args) {
		SpringApplication.run(McpApplication.class, args);
	}
}
