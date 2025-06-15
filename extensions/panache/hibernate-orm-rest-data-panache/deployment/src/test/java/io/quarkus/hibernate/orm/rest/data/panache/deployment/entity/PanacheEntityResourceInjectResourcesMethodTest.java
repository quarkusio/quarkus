package io.quarkus.hibernate.orm.rest.data.panache.deployment.entity;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.hibernate.orm.rest.data.panache.deployment.AbstractInjectResourcesMethodTest;
import io.quarkus.test.QuarkusUnitTest;

class PanacheEntityResourceInjectResourcesMethodTest extends AbstractInjectResourcesMethodTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().withApplicationRoot((jar) -> jar
            .addClasses(PanacheEntityBase.class, PanacheEntity.class, Collection.class, CollectionsResource.class,
                    AbstractEntity.class, AbstractItem.class, Item.class, ItemsResource.class, InjectionResource.class)
            .addAsResource("application.properties").addAsResource("import.sql"));
}
