package io.quarkus.cli.commands;

import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.completer.OptionCompleter;

public class DevModeDebugCompleter implements OptionCompleter<CompleterInvocation> {
    @Override
    public void complete(CompleterInvocation invocation) {
        if (invocation.getGivenCompleteValue() == null || invocation.getGivenCompleteValue().length() == 0) {
            invocation.addCompleterValue("false");
            invocation.addCompleterValue("true");
            invocation.addCompleterValue("client");
            invocation.addCompleterValue("{port}");
        } else {
            if ("false".startsWith(invocation.getGivenCompleteValue()))
                invocation.addCompleterValue("false");
            else if ("true".startsWith(invocation.getGivenCompleteValue()))
                invocation.addCompleterValue("true");
            else if ("client".startsWith(invocation.getGivenCompleteValue()))
                invocation.addCompleterValue("client");
        }
    }
}
