package io.quarkus.cli;

import picocli.AutoComplete.GenerateCompletion;
import picocli.CommandLine;

@CommandLine.Command(name = "completion", version = "generate-completion "
        + CommandLine.VERSION, mixinStandardHelpOptions = true, header = "bash/zsh completion:  source <(${PARENT-COMMAND-FULL-NAME:-$PARENTCOMMAND} ${COMMAND-NAME})", helpCommand = true, headerHeading = "%n", commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", optionListHeading = "Options:%n")
public class Completion extends GenerateCompletion {

}
