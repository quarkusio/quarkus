package io.quarkus.cli.commands;

import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.completer.OptionCompleter;

public class FormatCompleter implements OptionCompleter<CompleterInvocation> {

    @Override
    public void complete(CompleterInvocation invocation) {
        if (invocation.getGivenCompleteValue().length() == 0) {
            for (ExtensionFormat format : ExtensionFormat.values())
                invocation.addCompleterValue(format.formatValue());
        } else {
            for (ExtensionFormat format : ExtensionFormat.values()) {
                if (format.formatValue().startsWith(invocation.getGivenCompleteValue()))
                    invocation.addCompleterValue(format.formatValue());
            }
        }

    }
}
