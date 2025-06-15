package io.quarkus.test.junit.main;

/**
 * This interface is meant to be used on test methods as a method parameter. It allows the launching a command line
 * application with arbitrary parameters. See also {@link LaunchResult} and {@link Launch}
 */
public interface QuarkusMainLauncher {

    /**
     * Launch the command line application with the given parameters.
     */
    LaunchResult launch(String... args);

}
