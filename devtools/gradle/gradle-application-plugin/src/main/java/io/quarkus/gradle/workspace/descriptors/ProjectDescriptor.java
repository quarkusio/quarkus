package io.quarkus.gradle.workspace.descriptors;

import java.io.File;
import java.util.Set;

public interface ProjectDescriptor {

    public enum TaskType {
        COMPILE,
        RESOURCES
    }

    public File getProjectDir();

    public File getBuildDir();

    public File getBuildFile();

    public Set<String> getTasksForSourceSet(String sourceName);

    public String getTaskSource(String task);

    public String getTaskDestinationDir(String task);

    public TaskType getTaskType(String task);

}
