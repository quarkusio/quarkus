package io.quarkus.hibernate.accessor.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.accessor.ValueReader;
import org.hibernate.accessor.ValueWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class HibernateAccessorNestedClassTest extends HibernateAccessorBaseTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(
                    NestedEntitiesHost.class,
                    NestedEntitiesHost.InnerA.class,
                    NestedEntitiesHost.InnerA.DeepInner.class,
                    NestedEntitiesHost.InnerB.class));

    @Test
    void readInnerANameField() throws Exception {
        ValueReader<?> reader = reader(NestedEntitiesHost.InnerA.class, "name");

        NestedEntitiesHost.InnerA entity = new NestedEntitiesHost.InnerA("innerA", 5);
        assertThat(reader.get(entity)).isEqualTo("innerA");
    }

    @Test
    void writeInnerANameField() throws Exception {
        ValueReader<?> reader = reader(NestedEntitiesHost.InnerA.class, "name");
        ValueWriter writer = writer(NestedEntitiesHost.InnerA.class, "name");

        NestedEntitiesHost.InnerA entity = new NestedEntitiesHost.InnerA();
        writer.set(entity, "written-A");
        assertThat(reader.get(entity)).isEqualTo("written-A");
    }

    @Test
    void readInnerACountField() throws Exception {
        ValueReader<?> reader = reader(NestedEntitiesHost.InnerA.class, "count");

        NestedEntitiesHost.InnerA entity = new NestedEntitiesHost.InnerA("test", 77);
        assertThat(reader.get(entity)).isEqualTo(77);
    }

    @Test
    void readInnerBNameField() throws Exception {
        ValueReader<?> reader = reader(NestedEntitiesHost.InnerB.class, "name");

        NestedEntitiesHost.InnerB entity = new NestedEntitiesHost.InnerB("innerB", 3.14);
        assertThat(reader.get(entity)).isEqualTo("innerB");
    }

    @Test
    void writeInnerBNameField() throws Exception {
        ValueReader<?> reader = reader(NestedEntitiesHost.InnerB.class, "name");
        ValueWriter writer = writer(NestedEntitiesHost.InnerB.class, "name");

        NestedEntitiesHost.InnerB entity = new NestedEntitiesHost.InnerB();
        writer.set(entity, "written-B");
        assertThat(reader.get(entity)).isEqualTo("written-B");
    }

    @Test
    void readInnerBScoreField() throws Exception {
        ValueReader<?> reader = reader(NestedEntitiesHost.InnerB.class, "score");

        NestedEntitiesHost.InnerB entity = new NestedEntitiesHost.InnerB("test", 2.718);
        assertThat(reader.get(entity)).isEqualTo(2.718);
    }

    @Test
    void readDeepInnerNameField() throws Exception {
        ValueReader<?> reader = reader(NestedEntitiesHost.InnerA.DeepInner.class, "name");

        NestedEntitiesHost.InnerA.DeepInner entity = new NestedEntitiesHost.InnerA.DeepInner("deep", 100L);
        assertThat(reader.get(entity)).isEqualTo("deep");
    }

    @Test
    void writeDeepInnerNameField() throws Exception {
        ValueReader<?> reader = reader(NestedEntitiesHost.InnerA.DeepInner.class, "name");
        ValueWriter writer = writer(NestedEntitiesHost.InnerA.DeepInner.class, "name");

        NestedEntitiesHost.InnerA.DeepInner entity = new NestedEntitiesHost.InnerA.DeepInner();
        writer.set(entity, "written-deep");
        assertThat(reader.get(entity)).isEqualTo("written-deep");
    }

    @Test
    void readDeepInnerDeepValueField() throws Exception {
        ValueReader<?> reader = reader(NestedEntitiesHost.InnerA.DeepInner.class, "deepValue");

        NestedEntitiesHost.InnerA.DeepInner entity = new NestedEntitiesHost.InnerA.DeepInner("test", 999L);
        assertThat(reader.get(entity)).isEqualTo(999L);
    }

    @Test
    void sameNameFieldsInDifferentInnerClassesAreIndependent() throws Exception {
        ValueReader<?> aReader = reader(NestedEntitiesHost.InnerA.class, "name");
        ValueReader<?> bReader = reader(NestedEntitiesHost.InnerB.class, "name");
        ValueReader<?> deepReader = reader(NestedEntitiesHost.InnerA.DeepInner.class, "name");

        NestedEntitiesHost.InnerA a = new NestedEntitiesHost.InnerA("name-A", 0);
        NestedEntitiesHost.InnerB b = new NestedEntitiesHost.InnerB("name-B", 0.0);
        NestedEntitiesHost.InnerA.DeepInner deep = new NestedEntitiesHost.InnerA.DeepInner("name-deep", 0L);

        assertThat(aReader.get(a)).isEqualTo("name-A");
        assertThat(bReader.get(b)).isEqualTo("name-B");
        assertThat(deepReader.get(deep)).isEqualTo("name-deep");
    }

}
