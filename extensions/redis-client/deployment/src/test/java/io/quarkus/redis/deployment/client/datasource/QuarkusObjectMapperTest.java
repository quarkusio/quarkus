package io.quarkus.redis.deployment.client.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.jackson.JsonMapperBuilderCustomizer;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.hash.HashCommands;
import io.quarkus.redis.deployment.client.RedisTestResource;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;

@QuarkusTestResource(RedisTestResource.class)
public class QuarkusObjectMapperTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class).addClass(CustomCodecTest.Jedi.class).addClass(
                            CustomCodecTest.Sith.class)
                            .addClass(CustomCodecTest.CustomJediCodec.class).addClass(CustomCodecTest.CustomSithCodec.class))
            .overrideConfigKey("quarkus.redis.hosts", "${quarkus.redis.tr}");

    @Inject
    RedisDataSource ds;

    @Test
    public void test() {
        String key = UUID.randomUUID().toString();
        HashCommands<String, String, List<Person>> h = ds.hash(new TypeReference<>() {

        });
        h.hset(key, "test", List.of(new Person("foo", 100)));
        String stringRetrieved = ds.hash(String.class).hget(key, "test");
        assertThat(stringRetrieved).isEqualTo("[{\"nAmE\":\"foo\",\"aGe\":100}]");
        List<Person> peopleRetrieved = h.hget(key, "test");
        assertThat(peopleRetrieved).singleElement().satisfies(p -> {
            assertThat(p.getName()).isEqualTo("foo");
            assertThat(p.getAge()).isEqualTo(100);
        });
    }

    // without a custom module, this could not be deserialized as there are 2 constructors
    public static class Person {
        private final String name;
        private final int age;

        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @SuppressWarnings("unused")
        public Person(String name) {
            this.name = name;
            this.age = 0;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }
    }

    @Singleton
    public static class PersonCustomizer implements JsonMapperBuilderCustomizer {

        @Override
        public void customize(JsonMapper.Builder builder) {
            SimpleModule module = new SimpleModule();
            module.addDeserializer(Person.class, new PersonDeserializer());
            module.addSerializer(Person.class, new PersonSerializer());
            builder.addModule(module);
        }
    }

    public static class PersonSerializer extends StdSerializer<Person> {

        protected PersonSerializer() {
            super(Person.class);
        }

        @Override
        public void serialize(Person person, JsonGenerator jsonGenerator, SerializationContext context) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringProperty("nAmE", person.getName());
            jsonGenerator.writeNumberProperty("aGe", person.getAge());
            jsonGenerator.writeEndObject();
        }
    }

    public static class PersonDeserializer extends StdDeserializer<Person> {

        protected PersonDeserializer() {
            super(Person.class);
        }

        @Override
        public Person deserialize(JsonParser jsonParser, DeserializationContext context) {
            JsonNode node = jsonParser.objectReadContext().readTree(jsonParser);
            String name = node.get("nAmE").asText();
            int age = (Integer) node.get("aGe").numberValue();

            return new Person(name, age);
        }
    }
}
