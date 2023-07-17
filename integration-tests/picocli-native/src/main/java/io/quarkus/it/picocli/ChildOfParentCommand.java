package io.quarkus.it.picocli;

import picocli.CommandLine;

@CommandLine.Command(name = "child")
public class ChildOfParentCommand implements Runnable {

    @CommandLine.ParentCommand
    CommandUsedAsParent parent;

    @Override
    public void run() {
        System.out.println(parent.parentValue);
    }
}
