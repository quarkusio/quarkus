package io.quarkus.it.resteasy.jsonb;

import static io.quarkus.it.resteasy.jsonb.TestUtil.getConfiguredJsonb;
import static io.quarkus.it.resteasy.jsonb.TestUtil.getConfiguredJsonbSerializers;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.json.bind.serializer.JsonbSerializer;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ComplexObjectTest {

    @Test
    public void testJsonbResolverCreated() {
        assertThat(getConfiguredJsonb()).isNotNull();
    }

    @Test
    public void testJsonbConfigContainsCoffeeSerializer() {
        List<JsonbSerializer> configuredJsonbSerializers = getConfiguredJsonbSerializers();
        assertThat(configuredJsonbSerializers).anySatisfy(s -> {
            assertThat(s.getClass().getName()).contains("Coffee");
        });
    }

    @Test
    public void testSerialization() {
        Coffee coffee = new Coffee();
        coffee.setId(1);
        coffee.setName("Robusta");
        coffee.setCountryOfOrigin(new Country(1003, "Ethiopia", "ETH"));
        coffee.setSellers(Arrays.asList(new Seller("Carrefour", new Country(1001, "France", "FRA")),
                new Seller("Wallmart", new Country(1002, "USA", "USA"))));
        coffee.similarCoffees = Collections.singletonMap("arabica", 50);
        String jsonStr = getConfiguredJsonb().toJson(coffee);

        assertThat(jsonStr).isEqualTo(
                "{\"ID\":\"01.0\",\"enabled\":false,\"name\":\"Robusta\",\"nullLongValue\":null,\"origin\":{\"iso\":\"ETH\",\"id\":1003,\"name\":\"Ethiopia\"},\"sellers\":[{\"country\":{\"iso\":\"FRA\",\"id\":1001,\"name\":\"France\"},\"name\":\"Carrefour\"},{\"country\":{\"iso\":\"USA\",\"id\":1002,\"name\":\"USA\"},\"name\":\"Wallmart\"}],\"similar\":{\"arabica\":50}}");
    }
}
