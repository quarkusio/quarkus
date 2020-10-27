package io.quarkus.hibernate.orm.rest.data.panache.deployment.repository;

import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.rest.data.panache.PanacheRepositoryResource;
import io.quarkus.hibernate.orm.rest.data.panache.deployment.AbstractPathCustomisationTest;
import io.quarkus.rest.data.panache.MethodProperties;
import io.quarkus.rest.data.panache.ResourceProperties;
import io.quarkus.test.QuarkusUnitTest;

class PanacheRepositoryResourcePathCustomisationTest extends AbstractPathCustomisationTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Collection.class, CollectionsController.class, CollectionsRepository.class,
                            AbstractEntity.class, AbstractItem.class, Item.class, CustomPathCollectionsController.class)
                    .addAsResource("application.properties")
                    .addAsResource("import.sql"));

    @ResourceProperties(path = "custom-collections", hal = true)
    public interface CustomPathCollectionsController
            extends PanacheRepositoryResource<CollectionsRepository, Collection, String> {

        @MethodProperties(path = "api")
        Response list();

        @MethodProperties(path = "api")
        Collection get(String name);

        @MethodProperties(path = "api")
        Response add(Collection collection);

        @MethodProperties(path = "api")
        Response update(String name, Collection collection);

        @MethodProperties(path = "api")
        void delete(String name);
    }
}
