package com.example.adapters;


import com.example.ports.Tasks;
import dk.timer.mcp.config.UrlProperties;
import dk.timer.mcp.tools.model.TaskDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TaskAdapter implements Tasks {
    private final RestClient restClient;
    private final UrlProperties urlProperties;

    public List<TaskDTO> getTasksByProjectId(Long projectId) {
        return restClient.get()
                .uri(urlProperties.tasksApi(), projectId)
                .retrieve()
                .body(new ParameterizedTypeReference<List<TaskDTO>>() {
                });
    }
}
