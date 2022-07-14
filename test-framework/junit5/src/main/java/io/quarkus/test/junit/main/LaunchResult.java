package io.quarkus.test.junit.main;

import java.util.List;

/**
 * Contains information about a run (via {@link Launch}) of a command line application.
 * The class is meant to be used as a test method parameter giving the test the ability
 * to assert various aspects of the run.
 */
public interface LaunchResult {

    default String getOutput() {
        return String.join("\n", getOutputStream());
    }

    default String getErrorOutput() {
        return String.join("\n", getErrorStream());
    }

    default void echoSystemOut() {
        System.out.println(getOutput());
        System.out.println();
    }

    List<String> getOutputStream();

    List<String> getErrorStream();

    int exitCode();
}
