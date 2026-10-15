package io.quarkus.arc.test.clientproxy.proxynamecollision;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.arc.test.ArcTestContainer;

/**
 * Verifies that producer methods and fields declared in classes with the same
 * simple name but different packages do not produce colliding client proxy
 * class names.
 */
public class ProducerClientProxyNameCollisionTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(
            io.quarkus.arc.test.clientproxy.proxynamecollision.alpha.BeanProducer.class,
            io.quarkus.arc.test.clientproxy.proxynamecollision.beta.BeanProducer.class,
            io.quarkus.arc.test.clientproxy.proxynamecollision.alpha.FieldProducer.class,
            io.quarkus.arc.test.clientproxy.proxynamecollision.beta.FieldProducer.class,
            MyBean.class,
            Source.class);

    @Test
    public void testProducerMethodsWithSameSimpleClassNameInDifferentPackages() {
        ManagedContext requestContext = Arc.container().requestContext();
        requestContext.activate();
        try {
            MyBean alpha = Arc.container().instance(MyBean.class, new Source.Literal("alpha")).get();
            assertEquals("alpha", alpha.getSource());

            MyBean beta = Arc.container().instance(MyBean.class, new Source.Literal("beta")).get();
            assertEquals("beta", beta.getSource());
        } finally {
            requestContext.terminate();
        }
    }

    @Test
    public void testProducerFieldsWithSameSimpleClassNameInDifferentPackages() {
        ManagedContext requestContext = Arc.container().requestContext();
        requestContext.activate();
        try {
            MyBean alphaField = Arc.container().instance(MyBean.class, new Source.Literal("alphaField")).get();
            assertEquals("alphaField", alphaField.getSource());

            MyBean betaField = Arc.container().instance(MyBean.class, new Source.Literal("betaField")).get();
            assertEquals("betaField", betaField.getSource());
        } finally {
            requestContext.terminate();
        }
    }
}
