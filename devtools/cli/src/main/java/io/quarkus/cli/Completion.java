package io.quarkus.cli;

import java.util.concurrent.Callable;

import io.quarkus.quickcli.AutoComplete;
import io.quarkus.quickcli.CommandLine;
import io.quarkus.quickcli.CommandSpec;
import io.quarkus.quickcli.ExitCode;
import io.quarkus.quickcli.annotations.Command;
import io.quarkus.quickcli.annotations.Spec;

@Command(name = "completion", version = "generate-completion "
        + CommandLine.VERSION, header = "bash/zsh completion:  source <(${PARENT-COMMAND-FULL-NAME:-$PARENTCOMMAND} ${COMMAND-NAME})", helpCommand = true)
public class Completion implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Override
    public Integer call() {
        String script = AutoComplete.bash(
                spec.root().name(),
                spec.root());
        // not println: scripts with Windows line separators fail in strange ways
        System.out.print(script);
        System.out.print('\n');
        System.out.flush();
        return ExitCode.OK;
    }
}
