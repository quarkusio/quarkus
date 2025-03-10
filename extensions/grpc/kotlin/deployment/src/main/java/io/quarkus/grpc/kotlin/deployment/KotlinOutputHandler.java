package io.quarkus.grpc.kotlin.deployment;

import java.util.List;

import io.quarkus.grpc.codegen.AdditionalOutputHandler;

public class KotlinOutputHandler implements AdditionalOutputHandler {

    private static final String GENERATE_KOTLIN = "quarkus.generate-code.grpc.kotlin.generate";

    @Override
    public boolean supports(SupportsInput input) {
        return input.config().getOptionalValue(GENERATE_KOTLIN, Boolean.class).orElse(true);
    }

    @Override
    public HandleOutput handle(HandleInput input) {
        return new HandleOutput() {
            @Override
            public List<String> additionalOptions() {
                return List.of("--kotlin_out=" + input.outDir());
            }
        };
    }
}
