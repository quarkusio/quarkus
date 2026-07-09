package io.quarkus.deployment.key;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import io.quarkus.builder.item.EmptyBuildItem;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.annotations.Key;

public class KeyUtilsTest {

    public static class TestQualifier {
    }

    public static class OtherQualifier {
    }

    public static class NoKeyClass {
        String name;
    }

    public static class SingleKeyClass {
        @Key(TestQualifier.class)
        String name;
    }

    public static class TwoKeyClass {
        @Key(TestQualifier.class)
        String name;
        @Key(OtherQualifier.class)
        String other;
    }

    public static class NonStringKeyClass {
        @Key(TestQualifier.class)
        int name;
    }

    public static class StaticKeyClass {
        @Key(TestQualifier.class)
        static String name;
    }

    public static class KeyWithOtherFieldsClass {
        @Key(TestQualifier.class)
        String name;
        String value;
        int count;
    }

    @Test
    void noKeyFieldReturnsNull() {
        assertThat(KeyUtils.findKeyField(NoKeyClass.class)).isNull();
    }

    @Test
    void singleKeyFieldIsFound() {
        KeyDescriptor key = KeyUtils.findKeyField(SingleKeyClass.class);
        assertThat(key).isNotNull();
        assertThat(key.keyType()).isEqualTo(TestQualifier.class);
        assertThat(key.field().getName()).isEqualTo("name");
    }

    @Test
    void multipleKeyFieldsThrow() {
        assertThatThrownBy(() -> KeyUtils.findKeyField(TwoKeyClass.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Multiple @Key fields")
                .hasMessageContaining("name")
                .hasMessageContaining("other");
    }

    @Test
    void nonStringKeyFieldThrows() {
        assertThatThrownBy(() -> KeyUtils.findKeyField(NonStringKeyClass.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be of type String");
    }

    @Test
    void staticKeyFieldIsIgnored() {
        assertThat(KeyUtils.findKeyField(StaticKeyClass.class)).isNull();
    }

    @Test
    void keyFieldFoundAmongOtherFields() {
        KeyDescriptor key = KeyUtils.findKeyField(KeyWithOtherFieldsClass.class);
        assertThat(key).isNotNull();
        assertThat(key.keyType()).isEqualTo(TestQualifier.class);
        assertThat(key.field().getName()).isEqualTo("name");
    }

    public static final class KeyedMultiBuildItem extends MultiBuildItem {
        @Key(TestQualifier.class)
        String name;
    }

    public static final class KeyedSimpleBuildItem extends SimpleBuildItem {
        @Key(TestQualifier.class)
        String name;
    }

    public static final class KeyedEmptyBuildItem extends EmptyBuildItem {
        @Key(TestQualifier.class)
        String name;
    }

    @Test
    void keyOnMultiBuildItemIsAllowed() {
        KeyDescriptor key = KeyUtils.findKeyField(KeyedMultiBuildItem.class);
        assertThat(key).isNotNull();
        assertThat(key.keyType()).isEqualTo(TestQualifier.class);
    }

    @Test
    void keyOnSimpleBuildItemThrows() {
        assertThatThrownBy(() -> KeyUtils.findKeyField(KeyedSimpleBuildItem.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("only supported on MultiBuildItem");
    }

    @Test
    void keyOnEmptyBuildItemThrows() {
        assertThatThrownBy(() -> KeyUtils.findKeyField(KeyedEmptyBuildItem.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("only supported on MultiBuildItem");
    }

    @Test
    void getValue() throws Exception {
        KeyDescriptor key = KeyUtils.findKeyField(SingleKeyClass.class);
        SingleKeyClass instance = new SingleKeyClass();
        instance.name = "test-value";
        assertThat(key.getValue(instance)).isEqualTo("test-value");
    }
}
