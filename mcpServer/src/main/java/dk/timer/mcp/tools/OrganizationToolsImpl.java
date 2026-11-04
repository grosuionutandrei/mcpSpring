package dk.timer.mcp.tools;

import com.example.ports.OrganizationPort;
import dk.timer.mcp.spi.OrganizationTools;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrganizationToolsImpl implements OrganizationTools {
    private final OrganizationPort organizationPort;


    @McpTool(name = "Check_if_an_organization_exists_by_id",
            description = "Checks if an organization exists with the provided id",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = true, destructiveHint = false,
                    idempotentHint = true, openWorldHint = false))
    public String checkOrganizationExistance(@McpToolParam(description = "Id of the organization, e.g. 1") Long id) {

        if (organizationPort.checkIfExists(id)) {
            return "Organization exists in the system  ";
        }
        return "Organization does not exists in the system";
    }

}
