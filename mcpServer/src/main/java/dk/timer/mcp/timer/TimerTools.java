package dk.timer.mcp.timer;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class TimerTools {

	private final TimerRepository timerRepository;

	public record StartedTimer(Long id, String name, Instant startedAt) {
	}

	// Writes data (creates a timer) but never deletes or overwrites anything.
	// Not idempotent: every call creates a new timer.
	@McpTool(name = "startTimer",
			description = "Start a new timer with the given name and return its id and start time",
			annotations = @McpTool.McpAnnotations(
					readOnlyHint = false, destructiveHint = false,
					idempotentHint = false, openWorldHint = false))
	// No @Transactional here: it would make this bean a proxy, and the manually
	// built MCP server (McpServersConfig) can't see @McpTool on a proxy.
	// JpaRepository.save() already runs in its own transaction.
	public StartedTimer startTimer(@McpToolParam(description = "Name of the timer, e.g. 'pomodoro'") String name) {
		Timer timer = timerRepository.save(new Timer(name));
		return new StartedTimer(timer.getId(), timer.getName(), timer.getStartedAt());
	}

}
