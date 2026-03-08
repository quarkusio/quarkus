package io.quarkus.quickcli.deployment;

import java.util.concurrent.Callable;

import io.quarkus.quickcli.annotations.Command;
import io.quarkus.quickcli.annotations.Option;
import io.quarkus.quickcli.annotations.Parameters;

/**
 * Tests that the Quarkus bytecode transform makes private fields accessible.
 * No setters — relies on the deployment processor to remove ACC_PRIVATE
 * and replace _quickcli_set_* placeholder methods.
 */
@Command(name = "private-test", description = { "Tests private field access in Quarkus" })
public class PrivateFieldTestCommand implements Callable<Integer> {

    @Option(names = { "-n", "--name" }, description = "Name", required = true)
    private String name;

    @Parameters(index = "0", description = "Action to perform")
    private String action;

    @Override
    public Integer call() {
        System.out.println(action + ": " + name);
        return 0;
    }
}
