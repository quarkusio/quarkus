package io.quarkus.devservices.deployment.compose;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ComposeFiles {

    private final Map<String, ComposeServiceDefinition> serviceDefinitions;

    private final String projectName;
    private final List<File> files;

    public ComposeFiles(List<File> composeFiles) {
        this.serviceDefinitions = new HashMap<>();
        this.files = new ArrayList<>();
        String name = null;
        for (File composeFile : composeFiles) {
            ComposeFile compose = new ComposeFile(composeFile);
            if ((compose.getProjectName() == null) && compose.getServiceDefinitions().isEmpty()) {
                continue;
            }
            files.add(composeFile);
            for (Map.Entry<String, ComposeServiceDefinition> service : compose.getServiceDefinitions().entrySet()) {
                if (this.serviceDefinitions.containsKey(service.getKey())) {
                    throw new IllegalArgumentException("Service name conflict: " + service.getKey());
                }
                this.serviceDefinitions.put(service.getKey(), service.getValue());
            }
            if (name == null) {
                name = compose.getProjectName();
            }
        }
        projectName = name;
    }

    public Set<String> getAllServiceNames() {
        return serviceDefinitions.keySet();
    }

    public Map<String, ComposeServiceDefinition> getServiceDefinitions() {
        return serviceDefinitions;
    }

    public String getProjectName() {
        return projectName;
    }

    public List<File> getFiles() {
        return files;
    }
}
