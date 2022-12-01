package io.quarkus.hibernate.reactive.rest.data.panache.deployment.entity;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.reactive.rest.data.panache.deployment.AbstractCountMethodTest;
import io.quarkus.test.QuarkusUnitTest;

class PanacheEntityResourceCountMethodTest extends AbstractCountMethodTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Collection.class, CollectionsResource.class, AbstractEntity.class, AbstractItem.class,
                            Item.class, ItemsResource.class,
                            EmptyListItem.class, EmptyListItemsResource.class)
                    .addAsResource("application.properties")
                    .addAsResource("import.sql"));
}
