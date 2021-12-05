package io.quarkus.hibernate.orm;

import javax.inject.Inject;

import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.wildfly.common.Assert;

import io.quarkus.bootstrap.classloading.ClassLoaderLimiter;
import io.quarkus.hibernate.orm.enhancer.Address;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Let's run some checks to verify that the optimisations we have
 * to actually boot efficiently are going to survive other patches.
 */
public class JPAFastBootingTest {

    private static final ClassLoaderLimiter limitsChecker = ClassLoaderLimiter.builder()
            .neverLoadedResource("org/hibernate/jpa/orm_2_1.xsd")
            .neverLoadedResource("org/hibernate/jpa/orm_2_2.xsd")
            .neverLoadedClassName("org.hibernate.boot.jaxb.internal.MappingBinder")
            .build();

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(Address.class))
            .withConfigurationResource("application.properties")
            .addClassLoaderEventListener(limitsChecker);

    @Inject
    Session session;

    @Test
    public void testInjection() {
        //Check that Hibernate actually started:
        Assert.assertNotNull(session);
    }

}
