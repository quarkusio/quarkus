package io.quarkus.hibernate.orm.rest.data.panache.deployment.repository;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.rest.data.panache.deployment.AbstractDeleteMethodTest;
import io.quarkus.test.QuarkusExtensionTest;

class PanacheRepositoryResourceDeleteMethodTest extends AbstractDeleteMethodTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Collection.class, CollectionsResource.class, CollectionsRepository.class,
                            AbstractEntity.class, AbstractItem.class, Item.class, ItemsResource.class,
                            ItemsRepository.class)
                    .addAsResource("application.properties")
                    .addAsResource("import.sql"));
}
