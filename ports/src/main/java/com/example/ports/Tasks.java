package com.example.ports;

import dk.timer.mcp.tools.model.TaskDTO;

import java.util.List;

public interface Tasks {
    List<TaskDTO> getTasksByProjectId(Long projectId);
}
