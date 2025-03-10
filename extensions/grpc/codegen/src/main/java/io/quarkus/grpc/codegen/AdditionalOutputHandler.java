package io.quarkus.grpc.codegen;

import java.nio.file.Path;
import java.util.List;

import org.eclipse.microprofile.config.Config;

/**
 * Simple SPI that allows code to add additional options to the gRPC code generation invocation
 */
public interface AdditionalOutputHandler {

    /**
     * Whether this handler should be invoked for the given context
     */
    boolean supports(SupportsInput input);

    /**
     * Actually provide the additional options to be passed to the gRPC codegen
     */
    HandleOutput handle(HandleInput input);

    interface SupportsInput {

        Config config();
    }

    interface HandleInput {

        Path outDir();
    }

    interface HandleOutput {

        List<String> additionalOptions();
    }
}
