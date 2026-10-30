package io.quarkus.hibernate.accessor.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import org.hibernate.accessor.AccessorFactory;
import org.hibernate.accessor.Instantiator;
import org.hibernate.accessor.ValueReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class HibernateAccessorConstructorTest extends HibernateAccessorBaseTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(ConstructorEntity.class, PrivateConstructorEntity.class));

    @Test
    void validatesArgumentsAndWidensPrimitives() throws Exception {
        AccessorFactory factory = loadGeneratedFactory();
        Instantiator<ConstructorEntity> noArgs = factory.instantiator(ConstructorEntity.class.getDeclaredConstructor());
        assertThatThrownBy(() -> noArgs.create("extra"))
                .isInstanceOf(IllegalArgumentException.class);
        Instantiator<ConstructorEntity> constructor = factory.instantiator(
                ConstructorEntity.class.getDeclaredConstructor(String.class, int.class));
        assertThat(constructor.create("widened", (byte) 7).getValue()).isEqualTo(7);
        assertThatThrownBy(() -> constructor.create("missing"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> constructor.create("extra", 1, 2))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void noArgConstructor() throws Exception {
        AccessorFactory factory = loadGeneratedFactory();
        Constructor<ConstructorEntity> ctor = ConstructorEntity.class.getDeclaredConstructor();
        Instantiator<ConstructorEntity> instantiator = factory.instantiator(ctor);

        ConstructorEntity entity = instantiator.create();
        assertThat(entity).isNotNull();
        assertThat(entity.getName()).isNull();
        assertThat(entity.getValue()).isEqualTo(0);
    }

    @Test
    void parameterizedConstructor() throws Exception {
        AccessorFactory factory = loadGeneratedFactory();
        Constructor<ConstructorEntity> ctor = ConstructorEntity.class.getDeclaredConstructor(String.class, int.class);
        Instantiator<ConstructorEntity> instantiator = factory.instantiator(ctor);

        ConstructorEntity entity = instantiator.create("test", 42);
        assertThat(entity).isNotNull();
        assertThat(entity.getName()).isEqualTo("test");
        assertThat(entity.getValue()).isEqualTo(42);
    }

    @Test
    void privateNoArgConstructor() throws Exception {
        AccessorFactory factory = loadGeneratedFactory();
        Constructor<PrivateConstructorEntity> ctor = PrivateConstructorEntity.class.getDeclaredConstructor();
        Instantiator<PrivateConstructorEntity> instantiator = factory.instantiator(ctor);

        PrivateConstructorEntity entity = instantiator.create();
        assertThat(entity).isNotNull();
        assertThat(entity.getName()).isNull();
    }

    @Test
    void privateParameterizedConstructor() throws Exception {
        AccessorFactory factory = loadGeneratedFactory();
        Constructor<PrivateConstructorEntity> ctor = PrivateConstructorEntity.class.getDeclaredConstructor(String.class);
        Instantiator<PrivateConstructorEntity> instantiator = factory.instantiator(ctor);

        PrivateConstructorEntity entity = instantiator.create("private-ctor");
        assertThat(entity).isNotNull();
        assertThat(entity.getName()).isEqualTo("private-ctor");
    }

    @Test
    void constructorAndFieldAccessorsWorkTogether() throws Exception {
        // Verify that an instance created by the generated instantiator can be read and written
        // using field accessors obtained from the same factory.
        AccessorFactory factory = loadGeneratedFactory();
        Constructor<ConstructorEntity> ctor = ConstructorEntity.class.getDeclaredConstructor();
        Instantiator<ConstructorEntity> instantiator = factory.instantiator(ctor);

        ConstructorEntity entity = instantiator.create();
        Field nameField = ConstructorEntity.class.getDeclaredField("name");
        ValueReader<?> reader = factory.valueReader(nameField);
        assertThat(reader.get(entity)).isNull();

        factory.valueWriter(nameField).set(entity, "written");
        assertThat(reader.get(entity)).isEqualTo("written");
    }
}
