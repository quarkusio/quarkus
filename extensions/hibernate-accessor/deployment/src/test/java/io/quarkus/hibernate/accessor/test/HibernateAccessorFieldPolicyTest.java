package io.quarkus.hibernate.accessor.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.accessor.runtime.FieldAccess;
import io.quarkus.hibernate.accessor.runtime.ReflectionFreeAccessor;
import io.quarkus.hibernate.accessor.runtime.ReflectionFreeFieldAccessor;
import io.quarkus.test.QuarkusExtensionTest;

class HibernateAccessorFieldPolicyTest extends HibernateAccessorBaseTest {
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(Model.class));

    @Test
    void finalFieldBecomesWritableWhenRequested() throws Exception {
        Model model = new Model();
        writer(Model.class, "writable").set(model, "changed");
        assertThat(reader(Model.class, "writable").get(model)).isEqualTo("changed");
        assertThat(Modifier.isFinal(Model.class.getDeclaredField("writable").getModifiers())).isFalse();
    }

    @Test
    void defaultReadPreservesFinal() throws Exception {
        assertThat(reader(Model.class, "readOnly").get(new Model())).isEqualTo("original");
        assertThat(Modifier.isFinal(Model.class.getDeclaredField("readOnly").getModifiers())).isTrue();
        assertThatThrownBy(() -> writer(Model.class, "readOnly")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void writeOnlyDoesNotGenerateReader() throws Exception {
        Model model = new Model();
        writer(Model.class, "writeOnly").set(model, "changed");
        assertThat(model.writeOnly).isEqualTo("changed");
        assertThatThrownBy(() -> reader(Model.class, "writeOnly")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void disabledFieldHasNoAccessors() {
        assertThatThrownBy(() -> reader(Model.class, "disabled")).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> writer(Model.class, "disabled")).isInstanceOf(UnsupportedOperationException.class);
    }

    @ReflectionFreeAccessor
    public static class Model {
        @ReflectionFreeFieldAccessor(access = FieldAccess.READ_WRITE)
        private final String writable;
        private final String readOnly;
        @ReflectionFreeFieldAccessor(access = FieldAccess.WRITE)
        String writeOnly;
        @ReflectionFreeFieldAccessor(enabled = false)
        String disabled;

        public Model() {
            writable = "original";
            readOnly = "original";
        }
    }
}
