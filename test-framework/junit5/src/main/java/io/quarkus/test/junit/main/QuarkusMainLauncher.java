package io.quarkus.test.junit.main;

/**
 * The annotation is meant to be used on test methods only and it allows the launching a command line application with
 * arbitrary parameters.
 *
 * See also {@link LaunchResult} and {@link Launch}
 */
public interface QuarkusMainLauncher {

    LaunchResult launch(String... args);

}
