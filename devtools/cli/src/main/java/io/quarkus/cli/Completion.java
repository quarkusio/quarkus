package io.quarkus.cli;

import picocli.AutoComplete.GenerateCompletion;
import picocli.CommandLine;

@CommandLine.Command(name = "completion", version = "generate-completion "
        + CommandLine.VERSION, mixinStandardHelpOptions = true, description = {
                "bash/zsh completion:  source <(${PARENT-COMMAND-FULL-NAME:-$PARENTCOMMAND} ${COMMAND-NAME})",
                "Run the following command to give `${ROOT-COMMAND-NAME:-$PARENTCOMMAND}` TAB completion in the current shell:",
                "",
                "  source <(${PARENT-COMMAND-FULL-NAME:-$PARENTCOMMAND} ${COMMAND-NAME})",
                "" }, optionListHeading = "Options:%n", helpCommand = true)
public class Completion extends GenerateCompletion {

}
