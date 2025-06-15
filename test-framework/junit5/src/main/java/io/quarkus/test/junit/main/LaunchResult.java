package io.quarkus.test.junit.main;

import java.util.List;

/**
 * Contains information about a run (via {@link Launch} or {@link QuarkusMainLauncher}) of a command line application.
 * The class is meant to be used as a test method parameter giving the test the ability to assert various aspects of the
 * run.
 */
public interface LaunchResult {

    /**
     * Get the command line application standard output as a single string.
     */
    default String getOutput() {
        return String.join("\n", getOutputStream());
    }

    /**
     * Get the command line application error output as a single string.
     */
    default String getErrorOutput() {
        return String.join("\n", getErrorStream());
    }

    /**
     * Echo the command line application standard output to the console.
     */
    default void echoSystemOut() {
        System.out.println(getOutput());
        System.out.println();
    }

    /**
     * Get the command line application standard output as a list of strings. Each line of output correspond to a string
     * in the list.
     */
    List<String> getOutputStream();

    /**
     * Get the command line application error output as a list of strings. Each line of output correspond to a string in
     * the list.
     */
    List<String> getErrorStream();

    /**
     * Get the exit code of the application.
     */
    int exitCode();
}
