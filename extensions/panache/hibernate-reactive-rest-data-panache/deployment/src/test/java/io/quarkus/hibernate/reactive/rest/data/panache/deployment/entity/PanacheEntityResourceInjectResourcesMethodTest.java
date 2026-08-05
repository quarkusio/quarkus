package io.quarkus.hibernate.reactive.rest.data.panache.deployment.entity;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.quarkus.hibernate.reactive.rest.data.panache.deployment.AbstractInjectResourcesMethodTest;
import io.quarkus.test.QuarkusExtensionTest;

class PanacheEntityResourceInjectResourcesMethodTest extends AbstractInjectResourcesMethodTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(PanacheEntityBase.class, PanacheEntity.class, Collection.class, CollectionsResource.class,
                            AbstractEntity.class, AbstractItem.class, Item.class, ItemsResource.class, InjectionResource.class)
                    .addAsResource("application.properties")
                    .addAsResource("import.sql"));
}
