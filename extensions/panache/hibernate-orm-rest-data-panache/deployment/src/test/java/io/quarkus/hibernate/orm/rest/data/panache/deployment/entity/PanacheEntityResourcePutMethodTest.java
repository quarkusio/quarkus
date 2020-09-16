package io.quarkus.hibernate.orm.rest.data.panache.deployment.entity;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.rest.data.panache.deployment.AbstractPutMethodTest;
import io.quarkus.test.QuarkusUnitTest;

class PanacheEntityResourcePutMethodTest extends AbstractPutMethodTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Collection.class, CollectionsController.class, AbstractItem.class, Item.class,
                            ItemsController.class)
                    .addAsResource("application.properties")
                    .addAsResource("import.sql"));
}
