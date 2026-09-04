package io.quarkus.resteasy.reactive.jackson.deployment.processor;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.Type;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class JacksonDeserializerFactoryDiscoveryTest {

    @Test
    void primitiveFieldsOnly() {
        Index index = index(SimplePojo.class);
        Set<String> types = discover(index, SimplePojo.class);

        assertThat(types).containsExactlyInAnyOrder(SimplePojo.class.getName());
    }

    @Test
    void nestedObject() {
        Index index = index(Root.class, Nested.class);
        Set<String> types = discover(index, Root.class);

        assertThat(types).containsExactlyInAnyOrder(Root.class.getName(), Nested.class.getName());
    }

    @Test
    void listOfObjects() {
        Index index = index(WithList.class, Item.class);
        Set<String> types = discover(index, WithList.class);

        assertThat(types).containsExactlyInAnyOrder(WithList.class.getName(), Item.class.getName());
    }

    @Test
    void mapWithObjectValues() {
        Index index = index(WithMap.class, Item.class);
        Set<String> types = discover(index, WithMap.class);

        assertThat(types).containsExactlyInAnyOrder(WithMap.class.getName(), Item.class.getName());
    }

    @Test
    void ignoredFieldNotDiscovered() {
        Index index = index(WithIgnoredField.class, Item.class);
        Set<String> types = discover(index, WithIgnoredField.class);

        assertThat(types).containsExactlyInAnyOrder(WithIgnoredField.class.getName());
    }

    @Test
    void backReferenceFieldNotDiscovered() {
        Index index = index(WithBackReference.class, Item.class);
        Set<String> types = discover(index, WithBackReference.class);

        assertThat(types).containsExactlyInAnyOrder(WithBackReference.class.getName());
    }

    @Test
    void transitiveDiscovery() {
        Index index = index(Level1.class, Level2.class, Level3.class);
        Set<String> types = discover(index, Level1.class);

        assertThat(types).containsExactlyInAnyOrder(
                Level1.class.getName(), Level2.class.getName(), Level3.class.getName());
    }

    @Test
    void constructorParamsDiscovered() {
        Index index = index(WithCreator.class, Item.class);
        Set<String> types = discover(index, WithCreator.class);

        assertThat(types).containsExactlyInAnyOrder(WithCreator.class.getName(), Item.class.getName());
    }

    @Test
    void noConstructorNotDiscovered() {
        Index index = index(NoDefaultConstructor.class);
        Set<String> types = discover(index, NoDefaultConstructor.class);

        assertThat(types).isEmpty();
    }

    @Test
    void genericDtoTypeArgumentDiscovered() {
        Index index = index(PageDTO.class, OtherDTO.class);
        Map<String, Type> bindings = Map.of("T",
                Type.create(org.jboss.jandex.DotName.createSimple(OtherDTO.class.getName()), Type.Kind.CLASS));
        Set<String> types = discover(index, PageDTO.class, bindings);

        assertThat(types).containsExactlyInAnyOrder(
                PageDTO.class.getName(), OtherDTO.class.getName());
    }

    @Test
    void sealedInterfaceField() {
        Index index = index(WithSealedField.class, Shape.class, Circle.class, Square.class);
        Set<String> types = discover(index, WithSealedField.class);

        assertThat(types).containsExactlyInAnyOrder(
                WithSealedField.class.getName(), Circle.class.getName(), Square.class.getName());
    }

    // --- helpers ---

    private Set<String> discover(Index index, Class<?> rootClass) {
        return discover(index, rootClass, Map.of());
    }

    private Set<String> discover(Index index, Class<?> rootClass, Map<String, Type> typeBindings) {
        JacksonDeserializerFactory factory = new JacksonDeserializerFactory(item -> {
        }, index);
        ClassInfo rootType = index.getClassByName(rootClass.getName());
        return factory.discoverTypes(rootType, typeBindings);
    }

    private static Index index(Class<?>... classes) {
        Indexer indexer = new Indexer();
        for (Class<?> clazz : classes) {
            try (InputStream stream = JacksonDeserializerFactoryDiscoveryTest.class.getClassLoader()
                    .getResourceAsStream(clazz.getName().replace('.', '/') + ".class")) {
                indexer.index(stream);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return indexer.complete();
    }

    // --- model classes ---

    public static class SimplePojo {
        public String name;
        public int age;
    }

    public static class Root {
        public String name;
        public Nested nested;
    }

    public static class Nested {
        public String value;
    }

    public static class WithList {
        public List<Item> items;
    }

    public static class WithMap {
        public Map<String, Item> entries;
    }

    public static class Item {
        public String name;
    }

    public static class WithIgnoredField {
        public String name;
        @JsonIgnore
        public Item ignored;
    }

    public static class WithBackReference {
        public String name;
        @JsonBackReference
        public Item parent;
    }

    public static class Level1 {
        public Level2 next;
    }

    public static class Level2 {
        public Level3 deep;
    }

    public static class Level3 {
        public String value;
    }

    public static class WithCreator {
        public String name;
        public Item item;

        public WithCreator() {
        }

        @JsonCreator
        public WithCreator(@JsonProperty("name") String name, @JsonProperty("item") Item item) {
            this.name = name;
            this.item = item;
        }
    }

    public static class NoDefaultConstructor {
        public String value;

        public NoDefaultConstructor(String value) {
            this.value = value;
        }
    }

    public static class PageDTO<T> {
        public int number;
        public int size;
        public List<T> content;
    }

    public static class OtherDTO {
        public String value;
    }

    public sealed interface Shape permits Circle, Square {
    }

    public static final class Circle implements Shape {
        public double radius;
    }

    public static final class Square implements Shape {
        public double side;
    }

    public static class WithSealedField {
        public String name;
        public Shape shape;
    }
}
