package io.quarkus.hibernate.orm.rest.data.panache.deployment.entity;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.rest.data.panache.deployment.AbstractDevModeTest;
import io.quarkus.test.QuarkusDevModeTest;

public class PanacheEntityResourceDevModeTest extends AbstractDevModeTest {

    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Collection.class, AbstractEntity.class, AbstractItem.class, Item.class,
                            ItemsResource.class)
                    .addAsResource("application.properties")
                    .addAsResource("import.sql"));

}
