package io.quarkus.arc.test.config;

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.Converter;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;

public class ConfigMappingTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new StringAsset("config.my.prop=1234\n" +
                            "group.host=localhost\n" +
                            "group.port=8080\n" +
                            "types.int=9\n" +
                            "types.long=9999999999\n" +
                            "types.float=99.9\n" +
                            "types.double=99.99\n" +
                            "types.char=c\n" +
                            "types.boolean=true\n" +
                            "types.value=1234\n" +
                            "optionals.server.host=localhost\n" +
                            "optionals.server.port=8080\n" +
                            "optionals.optional=optional\n" +
                            "optionals.optional.int=9\n" +
                            "collections.strings=foo,bar\n" +
                            "collections.ints=1,2,3\n" +
                            "maps.server.host=localhost\n" +
                            "maps.server.port=8080\n" +
                            "maps.group.server.host=localhost\n" +
                            "maps.group.server.port=8080\n" +
                            "maps.base.server.host=localhost\n" +
                            "maps.base.server.port=8080\n" +
                            "maps.base.group.server.host=localhost\n" +
                            "maps.base.group.server.port=8080\n" +
                            "converters.foo=notbar\n" +
                            "override.server.host=localhost\n" +
                            "override.server.port=8080\n" +
                            "cloud.server.host=cloud\n" +
                            "cloud.server.port=9000\n" +
                            "cloud.server.port=9000\n" +
                            "hierarchy.foo=bar"),
                            "application.properties"));
    @Inject
    Config config;

    @ConfigMapping(prefix = "config")
    public interface MyConfigMapping {
        @WithName("my.prop")
        String myProp();
    }

    @Inject
    MyConfigMapping myConfigMapping;

    @Test
    void configMapping() {
        SmallRyeConfig smallRyeConfig = ((SmallRyeConfig) config);
        MyConfigMapping configMapping = smallRyeConfig.getConfigMapping(MyConfigMapping.class);
        assertNotNull(configMapping);
        assertEquals("1234", configMapping.myProp());

        assertNotNull(myConfigMapping);
        assertEquals("1234", myConfigMapping.myProp());
    }

    @ConfigMapping(prefix = "group")
    public interface GroupMapping {
        @WithParentName
        ServerHost host();

        @WithParentName
        ServerPort port();

        interface ServerHost {
            String host();
        }

        interface ServerPort {
            int port();
        }
    }

    @Inject
    GroupMapping groupMapping;

    @Test
    void groups() {
        assertNotNull(groupMapping);
        assertEquals("localhost", groupMapping.host().host());
        assertEquals(8080, groupMapping.port().port());
    }

    @ConfigMapping(prefix = "types")
    public interface SomeTypes {
        @WithName("int")
        int intPrimitive();

        @WithName("int")
        Integer intWrapper();

        @WithName("long")
        long longPrimitive();

        @WithName("long")
        Long longWrapper();

        @WithName("float")
        float floatPrimitive();

        @WithName("float")
        Float floatWrapper();

        @WithName("double")
        double doublePrimitive();

        @WithName("double")
        Double doubleWrapper();

        @WithName("char")
        char charPrimitive();

        @WithName("char")
        Character charWrapper();

        @WithName("boolean")
        boolean booleanPrimitive();

        @WithName("boolean")
        Boolean booleanWrapper();

        @WithName("value")
        ConfigValue configValue();
    }

    @Inject
    SomeTypes types;

    @Test
    void types() {
        assertNotNull(types);
        assertEquals(9, types.intPrimitive());
        assertEquals(9, types.intWrapper());
        assertEquals(9999999999L, types.longPrimitive());
        assertEquals(9999999999L, types.longWrapper());
        assertEquals(99.9f, types.floatPrimitive());
        assertEquals(99.9f, types.floatWrapper());
        assertEquals(99.99, types.doublePrimitive());
        assertEquals(99.99, types.doubleWrapper());
        assertEquals('c', types.charPrimitive());
        assertEquals('c', types.charWrapper());
        assertTrue(types.booleanPrimitive());
        assertTrue(types.booleanWrapper());
        assertEquals("1234", types.configValue().getValue());
    }

    @ConfigMapping(prefix = "optionals")
    public interface Optionals {
        Optional<Server> server();

        Optional<String> optional();

        @WithName("optional.int")
        OptionalInt optionalInt();

        interface Server {
            String host();

            int port();
        }
    }

    @Inject
    Optionals optionals;

    @Test
    void optionals() {
        assertTrue(optionals.server().isPresent());
        assertEquals("localhost", optionals.server().get().host());
        assertEquals(8080, optionals.server().get().port());

        assertTrue(optionals.optional().isPresent());
        assertEquals("optional", optionals.optional().get());
        assertTrue(optionals.optionalInt().isPresent());
        assertEquals(9, optionals.optionalInt().getAsInt());
    }

    @ConfigMapping(prefix = "collections")
    public interface Collections {
        @WithName("strings")
        List<String> listStrings();

        @WithName("ints")
        List<Integer> listInts();
    }

    @Inject
    Collections collections;

    @Test
    void collections() {
        assertEquals(Stream.of("foo", "bar").collect(toList()), collections.listStrings());
        assertEquals(Stream.of(1, 2, 3).collect(toList()), collections.listInts());
    }

    @ConfigMapping(prefix = "maps")
    public interface Maps {
        Map<String, String> server();

        Map<String, Server> group();

        interface Server {
            String host();

            int port();
        }
    }

    @Inject
    Maps maps;

    @Test
    void maps() {
        assertEquals("localhost", maps.server().get("host"));
        assertEquals(8080, Integer.valueOf(maps.server().get("port")));

        assertEquals("localhost", maps.group().get("server").host());
        assertEquals(8080, maps.group().get("server").port());
    }

    public interface ServerBase {
        Map<String, String> server();
    }

    @ConfigMapping(prefix = "maps.base")
    public interface MapsWithBase extends ServerBase {
        @Override
        Map<String, String> server();

        Map<String, Server> group();

        interface Server {
            String host();

            int port();
        }
    }

    @Inject
    MapsWithBase mapsWithBase;

    @Test
    void mapsWithBase() {
        assertEquals("localhost", mapsWithBase.server().get("host"));
        assertEquals(8080, Integer.valueOf(mapsWithBase.server().get("port")));

        assertEquals("localhost", mapsWithBase.group().get("server").host());
        assertEquals(8080, mapsWithBase.group().get("server").port());
    }

    @ConfigMapping(prefix = "defaults")
    public interface Defaults {
        @WithDefault("foo")
        String foo();

        @WithDefault("bar")
        String bar();
    }

    @Inject
    Defaults defaults;

    @Test
    void defaults() {
        assertEquals("foo", defaults.foo());
        assertEquals("bar", defaults.bar());
        assertEquals("foo", config.getValue("defaults.foo", String.class));

        final List<String> propertyNames = stream(config.getPropertyNames().spliterator(), false).collect(toList());
        assertFalse(propertyNames.contains("defaults.foo"));
    }

    @ConfigMapping(prefix = "converters")
    public interface Converters {
        @WithConverter(FooBarConverter.class)
        String foo();

        class FooBarConverter implements Converter<String> {
            @Override
            public String convert(final String value) {
                return "bar";
            }
        }
    }

    @Inject
    Converters converters;

    @Test
    void converters() {
        assertEquals("bar", converters.foo());
    }

    public interface Base {

        String foo();
    }

    @ConfigMapping(prefix = "hierarchy")
    public interface ExtendsBase extends Base {
    }

    @Inject
    Base base;

    @Inject
    ExtendsBase extendsBase;

    @Test
    void hierarchy() {
        assertSame(base, extendsBase);
        assertEquals("bar", extendsBase.foo());
    }

}
