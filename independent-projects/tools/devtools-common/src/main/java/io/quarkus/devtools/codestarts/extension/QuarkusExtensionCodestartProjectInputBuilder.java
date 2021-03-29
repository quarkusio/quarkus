package io.quarkus.devtools.codestarts.extension;

import io.quarkus.devtools.codestarts.CodestartProjectInputBuilder;
import io.quarkus.devtools.codestarts.extension.QuarkusExtensionCodestartCatalog.QuarkusExtensionData;
import io.quarkus.devtools.messagewriter.MessageWriter;
import java.util.Collection;
import java.util.Map;

public class QuarkusExtensionCodestartProjectInputBuilder extends CodestartProjectInputBuilder {
    boolean withoutIntegrationTests;
    boolean withoutUnitTest;
    boolean withoutDevModeTest;

    QuarkusExtensionCodestartProjectInputBuilder() {
        super();
    }

    public QuarkusExtensionCodestartProjectInputBuilder withoutIntegrationTests(boolean withoutIntegrationTest) {
        this.withoutIntegrationTests = withoutIntegrationTest;
        return this;
    }

    public QuarkusExtensionCodestartProjectInputBuilder withoutUnitTest(boolean withoutUnitTest) {
        this.withoutUnitTest = withoutUnitTest;
        return this;
    }

    public QuarkusExtensionCodestartProjectInputBuilder withoutDevModeTest(boolean withoutDevModeTest) {
        this.withoutDevModeTest = withoutDevModeTest;
        return this;
    }

    @Override
    public QuarkusExtensionCodestartProjectInputBuilder addCodestarts(Collection<String> codestarts) {
        super.addCodestarts(codestarts);
        return this;
    }

    @Override
    public QuarkusExtensionCodestartProjectInputBuilder addCodestart(String codestart) {
        super.addCodestart(codestart);
        return this;
    }

    @Override
    public QuarkusExtensionCodestartProjectInputBuilder addData(Map<String, Object> data) {
        super.addData(data);
        return this;
    }

    public QuarkusExtensionCodestartProjectInputBuilder putData(QuarkusExtensionData dataKey, Object value) {
        super.putData(dataKey, value);
        return this;
    }

    @Override
    public QuarkusExtensionCodestartProjectInputBuilder messageWriter(MessageWriter messageWriter) {
        super.messageWriter(messageWriter);
        return this;
    }

    public QuarkusExtensionCodestartProjectInput build() {
        return new QuarkusExtensionCodestartProjectInput(this);
    }

}
