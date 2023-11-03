package io.quarkus.hibernate.orm.rest.data.panache.deployment.entity;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.rest.data.panache.deployment.AbstractPutMethodTest;
import io.quarkus.test.QuarkusUnitTest;

class PanacheEntityResourcePutMethodTest extends AbstractPutMethodTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Collection.class, CollectionsResource.class, AbstractEntity.class, AbstractItem.class,
                            Item.class, ItemsResource.class)
                    .addAsResource("application.properties")
                    .addAsResource("import.sql"));
}
