package io.quarkus.hibernate.reactive.rest.data.panache.deployment.repository;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.reactive.rest.data.panache.deployment.AbstractDeleteMethodTest;
import io.quarkus.test.QuarkusUnitTest;

class PanacheRepositoryResourceDeleteMethodTest extends AbstractDeleteMethodTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Collection.class, CollectionsResource.class, CollectionsRepository.class,
                            AbstractEntity.class, AbstractItem.class, Item.class, ItemsResource.class,
                            ItemsRepository.class)
                    .addAsResource("application.properties")
                    .addAsResource("import.sql"));
}
