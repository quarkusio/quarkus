package io.quarkus.devtools.codestarts.extension;

import io.quarkus.devtools.codestarts.CodestartProjectInput;

public final class QuarkusExtensionCodestartProjectInput extends CodestartProjectInput {
    private final boolean withoutIntegrationTest;
    private final boolean withoutUnitTest;
    private final boolean withoutDevModeTest;

    public QuarkusExtensionCodestartProjectInput(QuarkusExtensionCodestartProjectInputBuilder builder) {
        super(builder);
        this.withoutIntegrationTest = builder.withoutIntegrationTest;
        this.withoutUnitTest = builder.withoutUnitTest;
        this.withoutDevModeTest = builder.withoutDevModeTest;
    }

    public static QuarkusExtensionCodestartProjectInputBuilder builder() {
        return new QuarkusExtensionCodestartProjectInputBuilder();
    }

    public boolean withoutIntegrationTest() {
        return withoutIntegrationTest;
    }

    public boolean withoutUnitTest() {
        return withoutUnitTest;
    }

    public boolean withoutDevModeTest() {
        return withoutDevModeTest;
    }
}
