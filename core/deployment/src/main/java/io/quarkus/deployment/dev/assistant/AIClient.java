package io.quarkus.deployment.dev.assistant;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import io.quarkus.deployment.dev.assistant.workspace.WorkspaceOutput;

public interface AIClient {

    // Workspace update (manipulate)
    default CompletableFuture<WorkspaceOutput> workspaceUpdate(String userMessage, Path path) {
        return workspaceUpdate(Optional.empty(), userMessage, Map.of(), path);
    }

    default CompletableFuture<WorkspaceOutput> workspaceUpdate(Optional<String> systemMessage, String userMessage,
            Path path) {
        return workspaceUpdate(systemMessage, userMessage, Map.of(), List.of(path));
    }

    default CompletableFuture<WorkspaceOutput> workspaceUpdate(String userMessage, List<Path> paths) {
        return workspaceUpdate(Optional.empty(), userMessage, Map.of(), paths);
    }

    default CompletableFuture<WorkspaceOutput> workspaceUpdate(Optional<String> systemMessage, String userMessage,
            List<Path> paths) {
        return workspaceUpdate(systemMessage, userMessage, Map.of(), paths);
    }

    default CompletableFuture<WorkspaceOutput> workspaceUpdate(String userMessageTemplate, Map<String, String> variables,
            Path path) {
        return workspaceUpdate(Optional.empty(), userMessageTemplate, variables, path);
    }

    default CompletableFuture<WorkspaceOutput> workspaceUpdate(Optional<String> systemMessageTemplate,
            String userMessageTemplate,
            Map<String, String> variables, Path path) {
        return workspaceUpdate(systemMessageTemplate, userMessageTemplate, variables, List.of(path));
    }

    default CompletableFuture<WorkspaceOutput> workspaceUpdate(String userMessageTemplate, Map<String, String> variables,
            List<Path> paths) {
        return workspaceUpdate(Optional.empty(), userMessageTemplate, variables, paths);
    }

    public CompletableFuture<WorkspaceOutput> workspaceUpdate(Optional<String> systemMessageTemplate,
            String userMessageTemplate,
            Map<String, String> variables, List<Path> paths);

    // Workspace create (generate)
    default CompletableFuture<WorkspaceOutput> workspaceCreate(String userMessage, Path path) {
        return workspaceCreate(Optional.empty(), userMessage, Map.of(), path);
    }

    default CompletableFuture<WorkspaceOutput> workspaceCreate(Optional<String> systemMessage, String userMessage,
            Path path) {
        return workspaceCreate(systemMessage, userMessage, Map.of(), List.of(path));
    }

    default CompletableFuture<WorkspaceOutput> workspaceCreate(String userMessage, List<Path> paths) {
        return workspaceCreate(Optional.empty(), userMessage, Map.of(), paths);
    }

    default CompletableFuture<WorkspaceOutput> workspaceCreate(Optional<String> systemMessage, String userMessage,
            List<Path> paths) {
        return workspaceCreate(systemMessage, userMessage, Map.of(), paths);
    }

    default CompletableFuture<WorkspaceOutput> workspaceCreate(String userMessageTemplate, Map<String, String> variables,
            Path path) {
        return workspaceCreate(Optional.empty(), userMessageTemplate, variables, path);
    }

    default CompletableFuture<WorkspaceOutput> workspaceCreate(Optional<String> systemMessageTemplate,
            String userMessageTemplate,
            Map<String, String> variables, Path path) {
        return workspaceCreate(systemMessageTemplate, userMessageTemplate, variables, List.of(path));
    }

    default CompletableFuture<WorkspaceOutput> workspaceCreate(String userMessageTemplate, Map<String, String> variables,
            List<Path> paths) {
        return workspaceCreate(Optional.empty(), userMessageTemplate, variables, paths);
    }

    public CompletableFuture<WorkspaceOutput> workspaceCreate(Optional<String> systemMessageTemplate,
            String userMessageTemplate,
            Map<String, String> variables,
            List<Path> paths);

    // Workspace Read (interpret)
    default CompletableFuture<WorkspaceOutput> workspaceRead(String userMessage, Path path) {
        return workspaceRead(Optional.empty(), userMessage, Map.of(), path);
    }

    default CompletableFuture<WorkspaceOutput> workspaceRead(Optional<String> systemMessage, String userMessage,
            Path path) {
        return workspaceRead(systemMessage, userMessage, Map.of(), List.of(path));
    }

    default CompletableFuture<WorkspaceOutput> workspaceRead(String userMessage, List<Path> paths) {
        return workspaceRead(Optional.empty(), userMessage, Map.of(), paths);
    }

    default CompletableFuture<WorkspaceOutput> workspaceRead(Optional<String> systemMessage, String userMessage,
            List<Path> paths) {
        return workspaceRead(systemMessage, userMessage, Map.of(), paths);
    }

    default CompletableFuture<WorkspaceOutput> workspaceRead(String userMessageTemplate, Map<String, String> variables,
            Path path) {
        return workspaceRead(Optional.empty(), userMessageTemplate, variables, path);
    }

    default CompletableFuture<WorkspaceOutput> workspaceRead(Optional<String> systemMessageTemplate,
            String userMessageTemplate,
            Map<String, String> variables, Path path) {
        return workspaceRead(systemMessageTemplate, userMessageTemplate, variables, List.of(path));
    }

    default CompletableFuture<WorkspaceOutput> workspaceRead(String userMessageTemplate, Map<String, String> variables,
            List<Path> paths) {
        return workspaceRead(Optional.empty(), userMessageTemplate, variables, paths);
    }

    public CompletableFuture<WorkspaceOutput> workspaceRead(Optional<String> systemMessageTemplate,
            String userMessageTemplate,
            Map<String, String> variables, List<Path> paths);

    // Workspace Dynamic
    default <T> CompletableFuture<T> workspaceDynamic(String userMessage, Path path) {
        return workspaceDynamic(Optional.empty(), userMessage, Map.of(), path);
    }

    default <T> CompletableFuture<T> workspaceDynamic(Optional<String> systemMessage, String userMessage,
            Path path) {
        return workspaceDynamic(systemMessage, userMessage, Map.of(), List.of(path));
    }

    default <T> CompletableFuture<T> workspaceDynamic(String userMessage, List<Path> paths) {
        return workspaceDynamic(Optional.empty(), userMessage, Map.of(), paths);
    }

    default <T> CompletableFuture<T> workspaceDynamic(Optional<String> systemMessage, String userMessage,
            List<Path> paths) {
        return workspaceDynamic(systemMessage, userMessage, Map.of(), paths);
    }

    default <T> CompletableFuture<T> workspaceDynamic(String userMessageTemplate, Map<String, String> variables,
            Path path) {
        return workspaceDynamic(Optional.empty(), userMessageTemplate, variables, path);
    }

    default <T> CompletableFuture<T> workspaceDynamic(Optional<String> systemMessageTemplate,
            String userMessageTemplate,
            Map<String, String> variables, Path path) {
        return workspaceDynamic(systemMessageTemplate, userMessageTemplate, variables, List.of(path));
    }

    default <T> CompletableFuture<T> workspaceDynamic(String userMessageTemplate, Map<String, String> variables,
            List<Path> paths) {
        return workspaceDynamic(Optional.empty(), userMessageTemplate, variables, paths);
    }

    public <T> CompletableFuture<T> workspaceDynamic(Optional<String> systemMessageTemplate,
            String userMessageTemplate,
            Map<String, String> variables, List<Path> paths);

    // Exception
    default CompletableFuture<ExceptionOutput> exception(String userMessage, String stacktrace, Path path) {
        return exception(Optional.empty(), userMessage, stacktrace, path);
    }

    public CompletableFuture<ExceptionOutput> exception(Optional<String> systemMessage, String userMessage, String stacktrace,
            Path path);

    // Dynamic (generic)
    default <T> CompletableFuture<T> dynamic(String userMessage) {
        return dynamic(Optional.empty(), userMessage, Map.of());
    }

    default <T> CompletableFuture<T> dynamic(Optional<String> systemMessage, String userMessage) {
        return dynamic(systemMessage, userMessage, Map.of());
    }

    default <T> CompletableFuture<T> dynamic(String userMessageTemplate, Map<String, String> variables) {
        return dynamic(Optional.empty(), userMessageTemplate, variables);
    }

    default <T> CompletableFuture<T> dynamic(Optional<String> systemMessageTemplate, String userMessageTemplate,
            Map<String, String> variables) {
        return workspaceDynamic(systemMessageTemplate, userMessageTemplate, variables, List.of());
    }
}
