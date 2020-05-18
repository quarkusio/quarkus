package io.quarkus.it.picocli;

import org.assertj.core.api.Assertions;

import picocli.CommandLine;

@CommandLine.Command(name = "child")
public class ChildOfParentCommand implements Runnable {

    @CommandLine.ParentCommand
    CommandUsedAsParent parent;

    @Override
    public void run() {
        Assertions.assertThat(parent.parentValue).isEqualTo("testValue");
    }
}
