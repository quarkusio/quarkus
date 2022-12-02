package io.quarkus.hibernate.validator.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.arc.Subclass;

/**
 * Tests class normalization, which translates into getting the "original" class, without all the specializations instances of
 * {@link io.quarkus.arc.Subclass}. <br />
 * There's no need to be a @{@link io.quarkus.test.junit.QuarkusTest} because no Quarkus related feature is required.
 */
class ArcProxyBeanMetaDataClassNormalizerTest {

    @Test
    @DisplayName("Normalize should return same class if beanClass isn't a Subclass.")
    void normalize_simpleClass() {
        Class<Original> expected = Original.class;

        assertEquals(expected, new ArcProxyBeanMetaDataClassNormalizer().normalize(Original.class));
    }

    @Test
    @DisplayName("Normalize should return 'superclass' if beanClass is the only Subclass in it's class hierarchy.")
    void normalize_oneSubClass() {
        Class<Original> expected = Original.class;

        assertEquals(expected, new ArcProxyBeanMetaDataClassNormalizer().normalize(FirstSubclass.class));
    }

    @Test
    @DisplayName("Normalize should return upmost superclass if beanClass has more than one Subclass in it's class hierarchy.")
    void normalize_multipleSubClasses() {
        Class<Original> expected = Original.class;

        assertEquals(expected, new ArcProxyBeanMetaDataClassNormalizer().normalize(SecondSubclass.class));
    }

    private static class Original {
    }

    /**
     * Simulates an object injected through @{@link javax.inject.Inject}.
     */
    private static class FirstSubclass extends Original implements Subclass {
    }

    /**
     * Simulates an object injected through @{@link io.quarkus.test.junit.mockito.InjectMock}
     * or @{@link io.quarkus.test.junit.mockito.InjectSpy}.
     */
    private static class SecondSubclass extends FirstSubclass {
    }
}
