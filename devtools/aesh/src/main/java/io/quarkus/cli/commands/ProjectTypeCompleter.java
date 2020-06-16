package io.quarkus.cli.commands;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.completer.OptionCompleter;

import io.quarkus.devtools.project.BuildTool;

public class ProjectTypeCompleter implements OptionCompleter<CompleterInvocation> {

    @Override
    public void complete(CompleterInvocation invocation) {
        if (invocation.getGivenCompleteValue().length() == 0)
            invocation.addAllCompleterValues(
                    Arrays.stream(BuildTool.values()).map(BuildTool::name).collect(Collectors.toList()));
        else {
            for (BuildTool tool : BuildTool.values()) {
                if (tool.name().startsWith(invocation.getGivenCompleteValue()))
                    invocation.addCompleterValue(tool.name());
                else if (tool.name().toLowerCase().startsWith(invocation.getGivenCompleteValue()))
                    invocation.addCompleterValue(tool.name().toLowerCase());
            }
        }
    }
}
