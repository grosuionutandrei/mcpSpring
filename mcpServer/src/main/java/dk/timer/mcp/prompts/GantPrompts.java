package dk.timer.mcp.prompts;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.example.ports.Projects;
import com.example.ports.Tasks;
import dk.timer.mcp.tools.model.ProjectDTO;
import dk.timer.mcp.tools.model.TaskDTO;
import dk.timer.mcp.spi.ProjectPrompts;
import io.modelcontextprotocol.spec.McpSchema.CompleteRequest;
import io.modelcontextprotocol.spec.McpSchema.CompleteResult;
import io.modelcontextprotocol.spec.McpSchema.CompleteResult.CompleteCompletion;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

import org.springframework.ai.mcp.annotation.McpArg;
import org.springframework.ai.mcp.annotation.McpComplete;
import org.springframework.ai.mcp.annotation.McpPrompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * One prompt with autocomplete for both of its arguments.
 *
 * User flow in the host:
 *   1. picks "Project status report" from the prompt list
 *   2. types "web" in the project field  -> completion suggests "42 · Website relaunch"
 *   3. types "r" in the focus field      -> completion suggests "risks"
 *   4. host calls prompts/get            -> statusReport() runs NOW and loads live data
 *   5. the returned text is inserted into the chat and the model writes the report
 *
 * Picked up by McpServersConfig because it implements {@link ProjectPrompts}.
 */
@Component
public class GantPrompts implements ProjectPrompts {

    private static final Pattern LEADING_ID = Pattern.compile("^\\s*(\\d+)");
    private static final List<String> FOCUS_OPTIONS = List.of("overview", "risks", "next-milestones");
    private static final int MAX_SUGGESTIONS = 100; // the spec caps one completion response at 100 values

    private final Projects projectPort;
    private final Tasks tasksPort;
    private final long organizationId;

    public GantPrompts(Projects projectPort,
                       Tasks tasksPort,
                       @Value("${timer.organization-id:1}") long organizationId) {
        this.projectPort = projectPort;
        this.tasksPort = tasksPort;
        this.organizationId = organizationId;
    }

    // ------------------------------------------------------------------ prompt

    @McpPrompt(name = "project_status_report",
            title = "Project status report",
            description = "Write a meeting-ready status report for one project, using live data")
    public GetPromptResult statusReport(
            @McpArg(name = "project",
                    description = "Start typing the project's name and pick a suggestion", required = true)
            String project,
            @McpArg(name = "focus",
                    description = "overview, risks or next-milestones (default overview)")
            String focus) {

        long projectId = parseProjectId(project);

        // Runs at selection time, so the prompt contains today's numbers.
        // The Projects port has no "get by id", so find it in the organization's list.
        ProjectDTO p = projects().stream()
                .filter(pr -> pr.getId() != null && pr.getId() == projectId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No project with id " + projectId + " in organization " + organizationId));

        List<TaskDTO> tasks = tasksPort.getTasksByProjectId(projectId);
        if (tasks == null) {
            tasks = List.of();
        }

        OffsetDateTime now = OffsetDateTime.now();
        List<TaskDTO> overdue = tasks.stream()
                .filter(t -> t.getEndDate() != null && t.getEndDate().isBefore(now))
                .filter(t -> t.getProgress() == null || t.getProgress() < 100)
                .toList();
        String overdueList = overdue.isEmpty() ? "none"
                : overdue.stream().map(TaskDTO::getTitle).collect(Collectors.joining(", "));

        String chosenFocus = FOCUS_OPTIONS.contains(focus) ? focus : "overview";

        String text = """
                Write a status report for project "%s" (id %d) for a team meeting.
                Focus: %s.

                Current data, fetched just now:
                - Period: %s to %s
                - Overall progress: %s%%
                - Tasks: %d in total, %d overdue (%s)

                Call getTasksByProjectId if you need more detail on specific tasks.
                Keep it under 200 words and end with the decisions the team needs to make.
                """.formatted(p.getTitle(), projectId, chosenFocus,
                p.getStartDate(), p.getEndDate(), p.getProgress(),
                tasks.size(), overdue.size(), overdueList);

        return new GetPromptResult("Status report for " + p.getTitle(),
                List.of(new PromptMessage(Role.USER, new TextContent(text))));
    }

    // -------------------------------------------------------------- completion

    /**
     * One method serves every argument of the prompt, so it switches on the
     * argument's name. Called on (roughly) every keystroke: keep it fast.
     */
    @McpComplete(prompt = "project_status_report")
    public CompleteResult complete(CompleteRequest request) {
        String typed = request.argument().value() == null ? ""
                : request.argument().value().trim().toLowerCase(Locale.ROOT);

        List<String> matches = switch (request.argument().name()) {
            case "project" -> projects().stream()
                    .filter(pr -> pr.getId() != null && pr.getTitle() != null)
                    .filter(pr -> pr.getTitle().toLowerCase(Locale.ROOT).contains(typed)
                            || String.valueOf(pr.getId()).startsWith(typed))
                    .sorted(Comparator.comparing(ProjectDTO::getTitle))
                    .map(pr -> pr.getId() + " · " + pr.getTitle())   // the id travels inside the value
                    .toList();
            case "focus" -> FOCUS_OPTIONS.stream().filter(f -> f.startsWith(typed)).toList();
            default -> List.of();
        };

        List<String> page = matches.stream().limit(MAX_SUGGESTIONS).toList();
        return new CompleteResult(new CompleteCompletion(page, matches.size(), matches.size() > page.size()));
    }

    // ----------------------------------------------------------------- helpers

    private List<ProjectDTO> projects() {
        List<ProjectDTO> projects = projectPort.getProjectsByOrganizationId(organizationId);
        return projects == null ? List.of() : projects;
    }

    /** Accepts "42", or a picked suggestion like "42 · Website relaunch". */
    private static long parseProjectId(String project) {
        Matcher m = LEADING_ID.matcher(project == null ? "" : project);
        if (!m.find()) {
            throw new IllegalArgumentException(
                    "Pick a project from the suggestions, e.g. \"42 · Website relaunch\"");
        }
        return Long.parseLong(m.group(1));
    }
}
