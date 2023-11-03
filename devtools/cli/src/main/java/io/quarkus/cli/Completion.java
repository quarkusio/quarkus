package io.quarkus.cli;

import picocli.AutoComplete.GenerateCompletion;
import picocli.CommandLine;

@CommandLine.Command(name = "completion", version = "generate-completion "
        + CommandLine.VERSION, header = "bash/zsh completion:  source <(${PARENT-COMMAND-FULL-NAME:-$PARENTCOMMAND} ${COMMAND-NAME})", helpCommand = true)
public class Completion extends GenerateCompletion {

}
