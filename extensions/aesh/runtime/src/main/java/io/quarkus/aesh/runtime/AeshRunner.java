package io.quarkus.aesh.runtime;

import jakarta.enterprise.context.Dependent;

import org.aesh.AeshRuntimeRunner;
import org.aesh.command.CommandResult;
import org.jboss.logging.Logger;

import io.quarkus.runtime.QuarkusApplication;

@Dependent
public class AeshRunner implements QuarkusApplication {

    private static final Logger LOG = Logger.getLogger(AeshRunner.class);

    private final AeshRuntimeRunnerFactory factory;

    public AeshRunner(AeshRuntimeRunnerFactory factory) {
        this.factory = factory;
    }

    @Override
    public int run(String... args) throws Exception {
        try {
            AeshRuntimeRunner commandRunner = factory.create();
            commandRunner.args(args);
            CommandResult result = commandRunner.execute();
            if (result == null || result.isSuccess()) {
                return 0;
            }
            // Aesh uses -1 for FAILURE; map negative values to 1 for Unix convention
            int exitCode = result.getResultValue();
            return exitCode > 0 ? exitCode : 1;
        } catch (Exception e) {
            LOG.error("Error executing command", e);
            return 1;
        }
    }
}
