package io.quarkus.hibernate.reactive.rest.data.panache.deployment.repository;

import java.util.List;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.reactive.rest.data.panache.PanacheRepositoryResource;
import io.quarkus.hibernate.reactive.rest.data.panache.deployment.AbstractPathCustomisationTest;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.quarkus.rest.data.panache.MethodProperties;
import io.quarkus.rest.data.panache.ResourceProperties;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

class PanacheRepositoryResourcePathCustomisationTest extends AbstractPathCustomisationTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Collection.class, CollectionsResource.class, CollectionsRepository.class,
                            AbstractEntity.class, AbstractItem.class, Item.class, CustomPathCollectionsResource.class)
                    .addAsResource("application.properties")
                    .addAsResource("import.sql"));

    @ResourceProperties(path = "custom-collections", hal = true)
    public interface CustomPathCollectionsResource
            extends PanacheRepositoryResource<CollectionsRepository, Collection, String> {

        @Override
        @MethodProperties(path = "api")
        Uni<List<Collection>> list(Page page, Sort sort);

        @Override
        @MethodProperties(path = "api")
        Uni<Collection> get(String name);

        @Override
        @MethodProperties(path = "api")
        Uni<Collection> add(Collection collection);

        @Override
        @MethodProperties(path = "api")
        Uni<Collection> update(String name, Collection collection);

        @Override
        @MethodProperties(path = "api")
        Uni<Boolean> delete(String name);
    }
}
