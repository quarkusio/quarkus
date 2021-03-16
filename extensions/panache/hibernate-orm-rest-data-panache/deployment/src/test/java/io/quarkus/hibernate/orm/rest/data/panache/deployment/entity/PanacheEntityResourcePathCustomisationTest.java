package io.quarkus.hibernate.orm.rest.data.panache.deployment.entity;

import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.rest.data.panache.PanacheEntityResource;
import io.quarkus.hibernate.orm.rest.data.panache.deployment.AbstractPathCustomisationTest;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.quarkus.rest.data.panache.MethodProperties;
import io.quarkus.rest.data.panache.ResourceProperties;
import io.quarkus.test.QuarkusUnitTest;

class PanacheEntityResourcePathCustomisationTest extends AbstractPathCustomisationTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Collection.class, CollectionsResource.class, AbstractEntity.class, AbstractItem.class,
                            Item.class, CustomPathCollectionsResource.class)
                    .addAsResource("application.properties")
                    .addAsResource("import.sql"));

    @ResourceProperties(path = "custom-collections", hal = true)
    public interface CustomPathCollectionsResource extends PanacheEntityResource<Collection, String> {

        @MethodProperties(path = "api")
        List<Collection> list(Page page, Sort sort);

        @MethodProperties(path = "api")
        Collection get(String name);

        @MethodProperties(path = "api")
        Collection add(Collection collection);

        @MethodProperties(path = "api")
        Collection update(String name, Collection collection);

        @MethodProperties(path = "api")
        boolean delete(String name);
    }
}
