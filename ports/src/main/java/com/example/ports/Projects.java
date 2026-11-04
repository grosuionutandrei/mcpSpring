package com.example.ports;

import dk.timer.mcp.tools.model.ProjectDTO;

import java.util.List;

public interface Projects {
    List<ProjectDTO> getProjectsByOrganizationId(Long organizationId);
}
