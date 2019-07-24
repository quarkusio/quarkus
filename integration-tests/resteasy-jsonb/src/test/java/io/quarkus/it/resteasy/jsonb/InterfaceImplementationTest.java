package io.quarkus.it.resteasy.jsonb;

import static io.quarkus.it.resteasy.jsonb.TestUtil.getConfiguredJsonbSerializers;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import javax.json.bind.serializer.JsonbSerializer;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class InterfaceImplementationTest {

    @Test
    public void testJsonbConfigContainsPersonSerializer() {
        List<JsonbSerializer> configuredJsonbSerializers = getConfiguredJsonbSerializers();
        assertThat(configuredJsonbSerializers).anySatisfy(s -> {
            assertThat(s.getClass().getName()).contains("Person");
        });
    }
}
