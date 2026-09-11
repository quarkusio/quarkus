package io.quarkus.test.aesh;

/**
 * A classloader-safe snapshot of one pipeline stage's outcome.
 * <p>
 * Aesh's {@code StageOutcome} carries an {@code Execution} reference that
 * cannot cross the split classloader boundary. This record captures only
 * primitive and {@link String} data that both classloaders share.
 * <p>
 * Instances are built by {@link AeshLauncherImpl} from the raw signal data
 * sent by {@code CliRunner} through the signal queue.
 *
 * @param commandName the command's simple class name (e.g. {@code "EchoCommand"})
 * @param stageIndex 0-based position in the pipeline
 * @param stageCount total number of stages in the pipeline
 * @param exitCode the stage's exit code (0 = success)
 * @param errorMessage the error message if the stage failed, or {@code null}
 * @param errorClass the fully qualified error class name, or {@code null}
 * @param durationMs wall-clock execution time in milliseconds
 */
public record StageResult(
        String commandName,
        int stageIndex,
        int stageCount,
        int exitCode,
        String errorMessage,
        String errorClass,
        long durationMs) {

    /**
     * Returns {@code true} if this stage completed successfully.
     */
    public boolean isSuccess() {
        return exitCode == 0;
    }

    @Override
    public String toString() {
        return "StageResult{command=" + commandName
                + ", index=" + stageIndex + "/" + stageCount
                + ", exitCode=" + exitCode
                + (errorMessage != null ? ", error=" + errorMessage : "")
                + ", duration=" + durationMs + "ms}";
    }
}
