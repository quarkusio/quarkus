package io.quarkus.devtools.commands.handlers;

import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;

public class ScaffoldCommandHandler implements QuarkusCommandHandler {

    @Override
    public QuarkusCommandOutcome execute(QuarkusCommandInvocation invocation) throws QuarkusCommandException {
        System.out.println("xpto teste" + invocation.getQuarkusProject().getProjectDirPath());
        return QuarkusCommandOutcome.success();
    }

}
