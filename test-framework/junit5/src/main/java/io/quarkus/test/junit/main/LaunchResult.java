package io.quarkus.test.junit.main;

import java.util.List;

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