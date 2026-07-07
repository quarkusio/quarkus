package io.quarkus.gradle.application.internal.codegen.worker;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import org.gradle.api.provider.ProviderFactory;
import org.gradle.util.GradleVersion;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.gradle.application.internal.codegen.CodegenOperations;
import io.quarkus.gradle.application.internal.codegen.CodegenRequest;
import io.quarkus.gradle.application.internal.execution.worker.ForkOptionsSnapshot;
import io.quarkus.gradle.application.internal.execution.worker.WorkerExecutionSupport;
import io.quarkus.gradle.tooling.ToolingUtils;

public final class WorkerBackedCodegenOperations implements CodegenOperations {

    private final WorkerExecutionSupport workerExecution;

    public WorkerBackedCodegenOperations(WorkerExecutor workerExecutor, ProviderFactory providers,
            ForkOptionsSnapshot codegenForkOptions, String pathEnvironment, String gradleWorkerMaxHeap) {
        this.workerExecution = new WorkerExecutionSupport(workerExecutor, providers, codegenForkOptions, pathEnvironment,
                gradleWorkerMaxHeap);
    }

    @Override
    public void generate(CodegenRequest request) {
        WorkerCodegenSubmission submission = workerCodegenSubmission(request);
        WorkQueue workQueue = workerExecution.workQueue(request.effectiveConfig().quarkusWorkerValues());
        workQueue.submit(CodegenWorker.class, params -> {
            params.getBuildSystemProperties().putAll(submission.buildSystemProperties());
            params.getForkedSystemProperties().putAll(submission.forkedSystemProperties());
            params.getProcessIsolated().set(submission.processIsolated());
            params.getBaseName().set(submission.baseName());
            params.getTargetDirectory().set(submission.targetDirectory().toFile());
            params.getAppModel().set(submission.appModel());
            params.getGradleVersion().set(submission.gradleVersion());
            params.getSourceDirectories().setFrom(request.sourceParentDirectories());
            params.getOutputPath().set(request.generatedSourcesDirectory().toFile());
            params.getLaunchMode().set(request.launchMode());
        });
        workQueue.await();
    }

    WorkerCodegenSubmission workerCodegenSubmission(CodegenRequest request) {
        return new WorkerCodegenSubmission(
                request.buildSystemProperties(),
                request.effectiveConfig().quarkusWorkerValues(),
                workerExecution.isProcessIsolated(),
                request.buildSystemProperties().getOrDefault("quarkus.package.output-name", request.projectDisplayName()),
                request.buildDirectory(),
                resolveAppModel(request.appModel()),
                GradleVersion.current().getVersion());
    }

    private static ApplicationModel resolveAppModel(Path appModel) {
        try {
            return ToolingUtils.deserializeAppModel(appModel);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    record WorkerCodegenSubmission(
            Map<String, String> buildSystemProperties,
            Map<String, String> forkedSystemProperties,
            boolean processIsolated,
            String baseName,
            Path targetDirectory,
            ApplicationModel appModel,
            String gradleVersion) {
    }
}
