package io.quarkus.gradle.workspace.descriptors;

import java.io.File;
import java.io.Serializable;

import io.quarkus.gradle.workspace.descriptors.ProjectDescriptor.TaskType;

public class QuarkusTaskDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String taskName;
    private final TaskType taskType;
    private final File sourceDir;
    private final File destinationDir;

    public QuarkusTaskDescriptor(String taskName, TaskType taskType, File sourceDir, File destinationDir) {
        this.taskName = taskName;
        this.taskType = taskType;
        this.sourceDir = sourceDir;
        this.destinationDir = destinationDir;
    }

    public String getTaskName() {
        return taskName;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public File getSourceDir() {
        return sourceDir;
    }

    public File getDestinationDir() {
        return destinationDir;
    }

    @Override
    public String toString() {
        return "QuarkusTaskDescriptor{" +
                "taskName='" + taskName + '\'' +
                ", taskType=" + taskType +
                ", sourceDir=" + sourceDir +
                ", destinationDir=" + destinationDir +
                '}';
    }
}
