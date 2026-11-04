package com.example.adapters;

import com.example.ports.OrganizationPort;
import dk.timer.mcp.config.UrlProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class OrganizationAdapter implements OrganizationPort {
    private final RestClient apiClient;
    private final UrlProperties properties;

    @Override
    public Boolean checkIfExists(Long organizationId) {
        return apiClient.get()
                .uri(properties.organizationApi(), organizationId)
                .retrieve()
                .body(Boolean.class);
    }
}
