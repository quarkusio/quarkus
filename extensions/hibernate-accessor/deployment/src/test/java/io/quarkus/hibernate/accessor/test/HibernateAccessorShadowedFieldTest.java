package io.quarkus.hibernate.accessor.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.accessor.ValueReader;
import org.hibernate.accessor.ValueWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class HibernateAccessorShadowedFieldTest extends HibernateAccessorBaseTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(ShadowedFieldBase.class, ShadowedFieldSub.class));

    @Test
    void readBaseNameField() throws Exception {
        ValueReader<?> reader = reader(ShadowedFieldBase.class, "name");

        ShadowedFieldSub entity = new ShadowedFieldSub("base-name", 10, "sub-name");
        assertThat(reader.get(entity)).isEqualTo("base-name");
    }

    @Test
    void readSubNameField() throws Exception {
        ValueReader<?> reader = reader(ShadowedFieldSub.class, "name");

        ShadowedFieldSub entity = new ShadowedFieldSub("base-name", 10, "sub-name");
        assertThat(reader.get(entity)).isEqualTo("sub-name");
    }

    @Test
    void baseAndSubNameFieldsAreIndependent() throws Exception {
        ValueReader<?> baseReader = reader(ShadowedFieldBase.class, "name");
        ValueReader<?> subReader = reader(ShadowedFieldSub.class, "name");

        ShadowedFieldSub entity = new ShadowedFieldSub("base", 0, "sub");
        assertThat(baseReader.get(entity)).isEqualTo("base");
        assertThat(subReader.get(entity)).isEqualTo("sub");
    }

    @Test
    void writeBaseNameFieldDoesNotAffectSubName() throws Exception {
        ValueReader<?> baseReader = reader(ShadowedFieldBase.class, "name");
        ValueReader<?> subReader = reader(ShadowedFieldSub.class, "name");
        ValueWriter baseWriter = writer(ShadowedFieldBase.class, "name");

        ShadowedFieldSub entity = new ShadowedFieldSub("base", 0, "sub");
        baseWriter.set(entity, "new-base");
        assertThat(baseReader.get(entity)).isEqualTo("new-base");
        assertThat(subReader.get(entity)).isEqualTo("sub");
    }

    @Test
    void writeSubNameFieldDoesNotAffectBaseName() throws Exception {
        ValueReader<?> baseReader = reader(ShadowedFieldBase.class, "name");
        ValueReader<?> subReader = reader(ShadowedFieldSub.class, "name");
        ValueWriter subWriter = writer(ShadowedFieldSub.class, "name");

        ShadowedFieldSub entity = new ShadowedFieldSub("base", 0, "sub");
        subWriter.set(entity, "new-sub");
        assertThat(subReader.get(entity)).isEqualTo("new-sub");
        assertThat(baseReader.get(entity)).isEqualTo("base");
    }
}
