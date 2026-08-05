package io.quarkus.hibernate.reactive.rest.data.panache.deployment.repository;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.reactive.rest.data.panache.deployment.AbstractPutMethodTest;
import io.quarkus.test.QuarkusExtensionTest;

class PanacheRepositoryResourcePutMethodTest extends AbstractPutMethodTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Collection.class, CollectionsResource.class, CollectionsRepository.class,
                            AbstractEntity.class, AbstractItem.class, Item.class, ItemsResource.class,
                            ItemsRepository.class)
                    .addAsResource("application.properties")
                    .addAsResource("import.sql"));
}
