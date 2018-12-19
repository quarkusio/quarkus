package org.jboss.shamrock.example.test;

import org.hibernate.SessionFactory;
import org.jboss.shamrock.test.ShamrockUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;

/**
 * For logging, run with: -Dorg.jboss.logging.provider=log4j2
 */
@Disabled
public class InfinispanCacheJPANonStrictCorrectnessTest {

    @Inject
    EntityManagerFactory entityManagerFactory;

    private InfinispanCacheJPACorrectnessTestCase testCase;

    @PostConstruct
    void init() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        testCase = new InfinispanCacheJPACorrectnessTestCase(sessionFactory, null, null, null);
    }

    @RegisterExtension
    static ShamrockUnitTest runner = new ShamrockUnitTest()
            .setArchiveProducer(() ->
                    ShrinkWrap.create(JavaArchive.class)
                        .addAsManifestResource("META-INF/nonstrict-persistence.xml", "persistence.xml")
                        .addAsManifestResource("META-INF/nonstrict-microprofile-config.properties", "microprofile-config.properties")
            );

    @Test
    public void test() throws Exception {
        testCase.test();
    }

}
