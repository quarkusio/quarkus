package io.quarkus.devtools.codestarts.extension;

import io.quarkus.devtools.codestarts.CodestartProjectInput;

public final class QuarkusExtensionCodestartProjectInput extends CodestartProjectInput {
    private final boolean withoutIntegrationTests;
    private final boolean withoutUnitTest;
    private final boolean withoutDevModeTest;

    public QuarkusExtensionCodestartProjectInput(QuarkusExtensionCodestartProjectInputBuilder builder) {
        super(builder);
        this.withoutIntegrationTests = builder.withoutIntegrationTests;
        this.withoutUnitTest = builder.withoutUnitTest;
        this.withoutDevModeTest = builder.withoutDevModeTest;
    }

    public static QuarkusExtensionCodestartProjectInputBuilder builder() {
        return new QuarkusExtensionCodestartProjectInputBuilder();
    }

    public boolean withoutIntegrationTests() {
        return withoutIntegrationTests;
    }

    public boolean withoutUnitTest() {
        return withoutUnitTest;
    }

    public boolean withoutDevModeTest() {
        return withoutDevModeTest;
    }
}
