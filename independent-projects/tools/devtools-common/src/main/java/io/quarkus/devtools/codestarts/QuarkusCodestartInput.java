package io.quarkus.devtools.codestarts;

import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;

public final class QuarkusCodestartInput {
    private final CodestartInput codestartInput;
    private final boolean noExamples;
    private final boolean noDockerfiles;
    private final boolean noBuildToolWrapper;

    QuarkusCodestartInput(QuarkusCodestartInputBuilder builder) {
        this.codestartInput = builder.inputBuilder.build();
        this.noExamples = builder.noExamples;
        this.noDockerfiles = builder.noDockerfiles;
        this.noBuildToolWrapper = builder.noBuildToolWrapper;
    }

    public static QuarkusCodestartInputBuilder builder(QuarkusPlatformDescriptor platformDescr) {
        return new QuarkusCodestartInputBuilder(platformDescr);
    }

    public CodestartInput getCodestartInput() {
        return codestartInput;
    }

    public boolean noExamples() {
        return noExamples;
    }

    public boolean noDockerfiles() {
        return noDockerfiles;
    }

    public boolean noBuildToolWrapper() {
        return noBuildToolWrapper;
    }
}
