package io.quarkus.it.picocli;

import org.assertj.core.api.Assertions;

import picocli.CommandLine;

@CommandLine.Command(name = "quarkus", versionProvider = DynamicVersionProvider.class)
public class DynamicVersionProviderCommand implements Runnable {

    @CommandLine.Spec
    CommandLine.Model.CommandSpec commandSpec;

    @Override
    public void run() {
        Assertions.assertThat(commandSpec.version()).containsExactly("quarkus version 1.0");
    }
}
