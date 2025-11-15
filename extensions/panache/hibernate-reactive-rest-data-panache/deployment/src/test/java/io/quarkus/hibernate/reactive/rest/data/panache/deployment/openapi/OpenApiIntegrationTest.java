package io.quarkus.hibernate.reactive.rest.data.panache.deployment.openapi;

import static org.hamcrest.Matchers.is;

import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.hibernate.reactive.rest.data.panache.deployment.repository.AbstractEntity;
import io.quarkus.hibernate.reactive.rest.data.panache.deployment.repository.AbstractItem;
import io.quarkus.hibernate.reactive.rest.data.panache.deployment.repository.Collection;
import io.quarkus.hibernate.reactive.rest.data.panache.deployment.repository.CollectionsRepository;
import io.quarkus.hibernate.reactive.rest.data.panache.deployment.repository.CollectionsResource;
import io.quarkus.hibernate.reactive.rest.data.panache.deployment.repository.EmptyListItem;
import io.quarkus.hibernate.reactive.rest.data.panache.deployment.repository.EmptyListItemsRepository;
import io.quarkus.hibernate.reactive.rest.data.panache.deployment.repository.EmptyListItemsResource;
import io.quarkus.hibernate.reactive.rest.data.panache.deployment.repository.Item;
import io.quarkus.hibernate.reactive.rest.data.panache.deployment.repository.ItemsRepository;
import io.quarkus.hibernate.reactive.rest.data.panache.deployment.repository.ItemsResource;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusProdModeTest;
import io.restassured.RestAssured;

class OpenApiIntegrationTest {

    private static final String OPEN_API_PATH = "/q/openapi";
    private static final String COLLECTIONS_SCHEMA_REF = "#/components/schemas/Collection";

    @RegisterExtension
    static final QuarkusProdModeTest TEST = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Collection.class, CollectionsResource.class, CollectionsRepository.class,
                            AbstractEntity.class, AbstractItem.class, Item.class, ItemsResource.class,
                            ItemsRepository.class, EmptyListItem.class, EmptyListItemsRepository.class,
                            EmptyListItemsResource.class)
                    .addAsResource("application.properties")
                    .addAsResource("import.sql"))
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-smallrye-openapi-deployment", Version.getVersion()),
                    Dependency.of("io.quarkus", "quarkus-reactive-pg-client-deployment", Version.getVersion()),
                    Dependency.of("io.quarkus", "quarkus-rest-jsonb-deployment", Version.getVersion()),
                    Dependency.of("io.quarkus", "quarkus-security-deployment", Version.getVersion())))
            .setRun(true);

    @Test
    public void testOpenApiForGeneratedResources() {
        RestAssured.given().queryParam("format", "JSON")
                .when().get(OPEN_API_PATH)
                .then()
                .header("Content-Type", "application/json;charset=UTF-8")
                .body("info.title", Matchers.equalTo("quarkus-hibernate-reactive-rest-data-panache-deployment API"))
                .body("paths.'/collections'", Matchers.hasKey("get"))
                .body("paths.'/collections'.get.tags", Matchers.hasItem("CollectionsResource"))
                .body("paths.'/collections'.get.responses.'200'.content.'application/json'.schema.type", is("array"))
                .body("paths.'/collections'.get.responses.'200'.content.'application/json'.schema.items.$ref",
                        is(COLLECTIONS_SCHEMA_REF))
                .body("paths.'/collections'", Matchers.hasKey("post"))
                .body("paths.'/collections'.post.tags", Matchers.hasItem("CollectionsResource"))
                .body("paths.'/collections'.post.requestBody.content.'application/json'.schema.$ref",
                        is(COLLECTIONS_SCHEMA_REF))
                .body("paths.'/collections'.post.responses.'201'.content.'application/json'.schema.$ref",
                        is(COLLECTIONS_SCHEMA_REF))
                .body("paths.'/collections'.post.security[0].SecurityScheme", Matchers.hasItem("user"))
                .body("paths.'/collections/{id}'", Matchers.hasKey("get"))
                .body("paths.'/collections/{id}'.get.responses.'200'.content.'application/json'.schema.$ref",
                        is(COLLECTIONS_SCHEMA_REF))
                .body("paths.'/collections/{id}'.get.security[0].SecurityScheme", Matchers.hasItem("user"))
                .body("paths.'/collections/{id}'", Matchers.hasKey("put"))
                .body("paths.'/collections/{id}'.put.requestBody.content.'application/json'.schema.$ref",
                        is(COLLECTIONS_SCHEMA_REF))
                .body("paths.'/collections/{id}'.put.responses.'201'.content.'application/json'.schema.$ref",
                        is(COLLECTIONS_SCHEMA_REF))
                .body("paths.'/collections/{id}'.put.security[0].SecurityScheme", Matchers.hasItem("user"))
                .body("paths.'/collections/{id}'", Matchers.hasKey("delete"))
                .body("paths.'/collections/{id}'.delete.responses", Matchers.hasKey("204"))
                .body("paths.'/collections/{id}'.delete.security[0].SecurityScheme", Matchers.hasItem("admin"))
                .body("paths.'/empty-list-items'", Matchers.hasKey("get"))
                .body("paths.'/empty-list-items'.get.tags", Matchers.hasItem("EmptyListItemsResource"))
                .body("paths.'/empty-list-items'", Matchers.hasKey("post"))
                .body("paths.'/empty-list-items'.post.tags", Matchers.hasItem("EmptyListItemsResource"))
                .body("paths.'/empty-list-items/{id}'", Matchers.hasKey("get"))
                .body("paths.'/empty-list-items/{id}'", Matchers.hasKey("put"))
                .body("paths.'/empty-list-items/{id}'", Matchers.hasKey("delete"))
                .body("paths.'/items'", Matchers.hasKey("get"))
                .body("paths.'/items'", Matchers.hasKey("post"))
                .body("paths.'/items/{id}'", Matchers.hasKey("get"))
                .body("paths.'/items/{id}'", Matchers.hasKey("put"))
                .body("paths.'/items/{id}'", Matchers.hasKey("delete"));
    }
}
