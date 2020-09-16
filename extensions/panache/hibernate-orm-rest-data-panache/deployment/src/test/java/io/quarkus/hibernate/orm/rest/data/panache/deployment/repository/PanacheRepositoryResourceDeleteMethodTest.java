package io.quarkus.hibernate.orm.rest.data.panache.deployment.repository;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.rest.data.panache.deployment.AbstractDeleteMethodTest;
import io.quarkus.test.QuarkusUnitTest;

class PanacheRepositoryResourceDeleteMethodTest extends AbstractDeleteMethodTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Collection.class, CollectionsController.class, CollectionsRepository.class, AbstractItem.class,
                            Item.class, ItemsController.class, ItemsRepository.class)
                    .addAsResource("application.properties")
                    .addAsResource("import.sql"));
}
