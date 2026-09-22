package io.quarkus.it.panache.next;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Regression test for hibernate/hibernate-orm#13090.
 * <p>
 * Verifies that the Hibernate Processor generates a compilable metamodel
 * class for a PanacheEntity with an inner interface that extends a
 * non-Panache type and has no {@code @Find}/{@code @HQL} methods.
 * <p>
 * The primary assertion is implicit: if the annotation processor emits
 * the inner interface type without a proper import, the generated
 * {@code EntityWithBareInnerInterface_} will not compile and this test
 * class will never load.
 */
@QuarkusTest
public class InnerInterfaceCdiAccessorTest {

    @Test
    void generatedMetamodelHasCdiAccessorForBareInnerInterface() throws Exception {
        Class<?> metamodel = Class.forName("io.quarkus.it.panache.next.EntityWithBareInnerInterface_");
        Method accessor = metamodel.getMethod("queries");

        assertThat(Modifier.isStatic(accessor.getModifiers()))
                .as("CDI accessor method 'queries()' should be static")
                .isTrue();
        assertThat(accessor.getReturnType())
                .as("CDI accessor should return the inner interface type")
                .isEqualTo(EntityWithBareInnerInterface.Queries.class);
    }
}
