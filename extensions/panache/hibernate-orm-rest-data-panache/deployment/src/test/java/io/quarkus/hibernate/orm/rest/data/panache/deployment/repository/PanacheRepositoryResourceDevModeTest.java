package io.quarkus.hibernate.orm.rest.data.panache.deployment.repository;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.rest.data.panache.deployment.AbstractDevModeTest;
import io.quarkus.test.QuarkusDevModeTest;

public class PanacheRepositoryResourceDevModeTest extends AbstractDevModeTest {

    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Collection.class, AbstractEntity.class, AbstractItem.class, Item.class,
                            ItemsResource.class, ItemsRepository.class)
                    .addAsResource("application.properties")
                    .addAsResource("import.sql"));

}
