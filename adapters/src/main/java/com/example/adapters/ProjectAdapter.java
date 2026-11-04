package com.example.adapters;

import com.example.ports.Projects;
import dk.timer.mcp.config.UrlProperties;
import dk.timer.mcp.tools.model.ProjectDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectAdapter implements Projects {
    private final RestClient restClient;
    private final UrlProperties urlProperties;

    @Override
    public List<ProjectDTO> getProjectsByOrganizationId(Long organizationId) {
        List<ProjectDTO> projects = restClient.get()
                .uri(urlProperties.projectsApi(), organizationId)
                .retrieve()
                .body(new ParameterizedTypeReference<List<ProjectDTO>>() {
                });
        return projects;
    }
}
