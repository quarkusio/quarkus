package io.quarkus.it.picocli;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine;

@TopCommand
@CommandLine.Command(name = "test", mixinStandardHelpOptions = true, commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", optionListHeading = "%nOptions:%n", subcommands = {
        CommandUsedAsParent.class,
        CompletionReflectionCommand.class,
        DefaultValueProviderCommand.class,
        DynamicVersionProviderCommand.class,
        LocalizedCommandOne.class,
        LocalizedCommandTwo.class,
        MutuallyExclusiveOptionsCommand.class,
        TestCommand.class,
        UnmatchedCommand.class,
        DynamicProxyInvokerCommand.class,
        WithMethodSubCommand.class })
public class TopTestCommand {

}
