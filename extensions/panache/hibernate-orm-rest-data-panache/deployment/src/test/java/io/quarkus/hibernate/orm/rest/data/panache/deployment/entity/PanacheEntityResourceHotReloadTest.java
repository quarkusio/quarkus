package io.quarkus.hibernate.orm.rest.data.panache.deployment.entity;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.rest.data.panache.deployment.AbstractHotReloadTest;
import io.quarkus.test.QuarkusDevModeTest;

public class PanacheEntityResourceHotReloadTest extends AbstractHotReloadTest {

    @RegisterExtension
    public final static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Collection.class, CollectionsResource.class, AbstractEntity.class, AbstractItem.class,
                            Item.class)
                    .addAsResource("application.properties")
                    .addAsResource("import.sql"));

    @Override
    protected QuarkusDevModeTest getTestArchive() {
        return TEST;
    }

    @Override
    protected Class<?> getResourceClass() {
        return CollectionsResource.class;
    }
}
