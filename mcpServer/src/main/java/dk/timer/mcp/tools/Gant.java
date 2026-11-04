package dk.timer.mcp.tools;

import com.example.ports.Projects;
import com.example.ports.Tasks;
import dk.timer.mcp.tools.model.ProjectDTO;
import dk.timer.mcp.tools.model.TaskDTO;
import dk.timer.mcp.spi.ProjectTools;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class Gant  implements ProjectTools {
    private final Projects projectPort;
    private final Tasks tasksPort;
    @McpTool(name = "getProjectsByOrganizationId",
            description = "Get the names of the active projects of an existing organization, by organization id",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = true, destructiveHint = false,
                    idempotentHint = true, openWorldHint = false))
    public List<String> getProjects(@McpToolParam(description = "Id of the organization, e.g. 1") Long id) {
        List<ProjectDTO> projects = projectPort.getProjectsByOrganizationId(id);

        return projects == null ? List.of() : projects.stream()
                .map(ProjectDTO::getTitle)
                .toList();
    }

    @McpTool(title = "Project tasks", name = "getTasksByProjectId",
            description = "Get the tasks asssociated with a project ",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = true, destructiveHint = false,
                    idempotentHint = true, openWorldHint = false))
    public List<TaskDTO> getTasks(@McpToolParam(description = "Id of the project, e.g. 1") Long projectId) {
        List<TaskDTO> tasks = tasksPort.getTasksByProjectId(projectId);
        return tasks == null ? List.of() : tasks;
    }
}
