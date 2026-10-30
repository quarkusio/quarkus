package io.quarkus.hibernate.accessor.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.accessor.ValueReader;
import org.hibernate.accessor.ValueWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class HibernateAccessorDeepHierarchyTest extends HibernateAccessorBaseTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(
                    DeepHierarchyGrandparent.class,
                    DeepHierarchyParent.class,
                    DeepHierarchyChild.class));

    @Test
    void readGrandparentFieldFromChild() throws Exception {
        ValueReader<?> reader = reader(DeepHierarchyGrandparent.class, "grandparentField");

        DeepHierarchyChild child = new DeepHierarchyChild("gp", "p", "c");
        assertThat(reader.get(child)).isEqualTo("gp");
    }

    @Test
    void writeGrandparentFieldFromChild() throws Exception {
        ValueReader<?> reader = reader(DeepHierarchyGrandparent.class, "grandparentField");
        ValueWriter writer = writer(DeepHierarchyGrandparent.class, "grandparentField");

        DeepHierarchyChild child = new DeepHierarchyChild();
        writer.set(child, "written-gp");
        assertThat(reader.get(child)).isEqualTo("written-gp");
    }

    @Test
    void readParentFieldFromChild() throws Exception {
        ValueReader<?> reader = reader(DeepHierarchyParent.class, "parentField");

        DeepHierarchyChild child = new DeepHierarchyChild("gp", "p", "c");
        assertThat(reader.get(child)).isEqualTo("p");
    }

    @Test
    void writeParentFieldFromChild() throws Exception {
        ValueReader<?> reader = reader(DeepHierarchyParent.class, "parentField");
        ValueWriter writer = writer(DeepHierarchyParent.class, "parentField");

        DeepHierarchyChild child = new DeepHierarchyChild();
        writer.set(child, "written-p");
        assertThat(reader.get(child)).isEqualTo("written-p");
    }

    @Test
    void readChildOwnField() throws Exception {
        ValueReader<?> reader = reader(DeepHierarchyChild.class, "childField");

        DeepHierarchyChild child = new DeepHierarchyChild("gp", "p", "c");
        assertThat(reader.get(child)).isEqualTo("c");
    }

    @Test
    void writeChildOwnField() throws Exception {
        ValueReader<?> reader = reader(DeepHierarchyChild.class, "childField");
        ValueWriter writer = writer(DeepHierarchyChild.class, "childField");

        DeepHierarchyChild child = new DeepHierarchyChild();
        writer.set(child, "written-c");
        assertThat(reader.get(child)).isEqualTo("written-c");
    }

    @Test
    void readAllThreeLevels() throws Exception {
        ValueReader<?> gpReader = reader(DeepHierarchyGrandparent.class, "grandparentField");
        ValueReader<?> pReader = reader(DeepHierarchyParent.class, "parentField");
        ValueReader<?> cReader = reader(DeepHierarchyChild.class, "childField");

        DeepHierarchyChild child = new DeepHierarchyChild("level-0", "level-1", "level-2");
        assertThat(gpReader.get(child)).isEqualTo("level-0");
        assertThat(pReader.get(child)).isEqualTo("level-1");
        assertThat(cReader.get(child)).isEqualTo("level-2");
    }

}
