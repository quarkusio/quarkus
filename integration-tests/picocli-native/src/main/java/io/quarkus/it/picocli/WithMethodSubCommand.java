package io.quarkus.it.picocli;

import org.assertj.core.api.Assertions;

import picocli.CommandLine;

@CommandLine.Command
public class WithMethodSubCommand {

    @CommandLine.Command
    void hello(@CommandLine.Option(names = { "-n", "--names" }, description = "Parameter option") String name) {
        Assertions.assertThat(name).isEqualTo("World!");
    }

    @CommandLine.Command
    void goodBye(@CommandLine.Mixin NameMixin mixin) {
        Assertions.assertThat(mixin.name).isEqualTo("Test?");
    }
}
