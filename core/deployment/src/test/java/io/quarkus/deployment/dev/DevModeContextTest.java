package io.quarkus.deployment.dev;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;

import org.junit.jupiter.api.Test;

class DevModeContextTest {

    @Test
    void buildUpdateSourceDefaultsToQuarkus() {
        assertThat(new DevModeContext().getBuildUpdateSource()).isEqualTo(DevModeContext.BuildUpdateSource.QUARKUS);
    }

    @Test
    void buildUpdateSourceCanBeSetToExternalBuildTool() {
        var context = new DevModeContext();

        context.setBuildUpdateSource(DevModeContext.BuildUpdateSource.EXTERNAL_BUILD_TOOL);

        assertThat(context.getBuildUpdateSource()).isEqualTo(DevModeContext.BuildUpdateSource.EXTERNAL_BUILD_TOOL);
    }

    @Test
    void buildUpdateSourceSurvivesSerialization() throws Exception {
        var context = new DevModeContext();
        context.setBuildUpdateSource(DevModeContext.BuildUpdateSource.EXTERNAL_BUILD_TOOL);

        var copy = serializeAndDeserialize(context);

        assertThat(copy.getBuildUpdateSource()).isEqualTo(DevModeContext.BuildUpdateSource.EXTERNAL_BUILD_TOOL);
    }

    @Test
    void buildUpdateSourceTreatsNullAsQuarkus() {
        var context = new DevModeContext();

        context.setBuildUpdateSource(null);

        assertThat(context.getBuildUpdateSource()).isEqualTo(DevModeContext.BuildUpdateSource.QUARKUS);
    }

    @Test
    void externalBuildOutputTransportDefaultsToDisabled() {
        var transport = new DevModeContext().getExternalBuildOutputTransport();

        assertThat(transport.isEnabled()).isFalse();
        assertThat(transport.getUri()).isEmpty();
        assertThat(transport.getToken()).isEmpty();
    }

    @Test
    void externalBuildOutputTransportTreatsNullAsDisabled() {
        var context = new DevModeContext();

        context.setExternalBuildOutputTransport(null);

        assertThat(context.getExternalBuildOutputTransport().isEnabled()).isFalse();
        assertThat(context.getExternalBuildOutputTransport().getUri()).isEmpty();
    }

    @Test
    void externalBuildOutputTransportUriTreatsNullAsDisabled() {
        var transport = DevModeContext.ExternalBuildOutputTransport.of(URI.create("tcp://127.0.0.1:12345"), "secret");

        transport.setUri(null);

        assertThat(transport.isEnabled()).isFalse();
        assertThat(transport.getUri()).isEmpty();
    }

    @Test
    void externalBuildOutputTransportCanBeSetToUri() {
        var context = new DevModeContext();

        context.setExternalBuildOutputTransport(
                DevModeContext.ExternalBuildOutputTransport.of(URI.create("tcp://127.0.0.1:12345"), "secret"));

        var transport = context.getExternalBuildOutputTransport();
        assertThat(transport.isEnabled()).isTrue();
        assertThat(transport.getUri()).contains(URI.create("tcp://127.0.0.1:12345"));
        assertThat(transport.getToken()).contains("secret");
    }

    @Test
    void externalBuildOutputTransportSurvivesSerialization() throws Exception {
        var context = new DevModeContext();
        context.setExternalBuildOutputTransport(
                DevModeContext.ExternalBuildOutputTransport.of(URI.create("tcp://127.0.0.1:12345"), "secret"));

        var copy = serializeAndDeserialize(context);

        var transport = copy.getExternalBuildOutputTransport();
        assertThat(transport.isEnabled()).isTrue();
        assertThat(transport.getUri()).contains(URI.create("tcp://127.0.0.1:12345"));
        assertThat(transport.getToken()).contains("secret");
    }

    private static DevModeContext serializeAndDeserialize(DevModeContext context) throws Exception {
        var output = new ByteArrayOutputStream();
        try (var objectOutput = new ObjectOutputStream(output)) {
            objectOutput.writeObject(context);
        }
        try (var objectInput = new ObjectInputStream(new ByteArrayInputStream(output.toByteArray()))) {
            return (DevModeContext) objectInput.readObject();
        }
    }
}
