package io.quarkus.aesh.runtime;

import jakarta.enterprise.context.Dependent;

import org.aesh.AeshRuntimeRunner;
import org.aesh.command.CommandResult;
import org.jboss.logging.Logger;

import io.quarkus.runtime.QuarkusApplication;

@Dependent
public class AeshRunner implements QuarkusApplication {

    private static final Logger LOG = Logger.getLogger(AeshRunner.class);

    private final AeshRuntimeRunner commandRunner;

    public AeshRunner(AeshRuntimeRunner commandRunner) {
        this.commandRunner = commandRunner;
    }

    @Override
    public int run(String... args) throws Exception {
        try {
            commandRunner.args(args);
            CommandResult result = commandRunner.execute();
            if (result == null || result == CommandResult.SUCCESS) {
                return 0;
            } else {
                return 1;
            }
        } catch (Exception e) {
            LOG.error("Error executing command", e);
            return 1;
        }
    }
}
