package io.quarkus.mongodb.panache.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.bson.Document;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.conversions.Bson;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;

import io.quarkus.mongodb.panache.common.reactive.runtime.ReactiveMongoOperations;
import io.quarkus.mongodb.panache.common.runtime.MongoOperations;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.mongodb.MongoReplicaSetTestResource;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@QuarkusTestResource(MongoReplicaSetTestResource.class)
@DisabledOnOs(OS.WINDOWS)
public class MongoDatabaseResolverTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(
                            "quarkus.arc.remove-unused-beans=false\n" +
                                    "quarkus.mongodb.connection-string=mongodb://localhost:27017,localhost:27018\n" +
                                    "quarkus.mongodb.devservices.enabled=false"),
                            "application.properties"));

    protected static final MongoOperations<Object, PanacheUpdate> OPERATIONS = new CustomMongoOperations();
    protected static final ReactiveMongoOperations<Object, PanacheUpdate> REACTIVE_OPERATIONS = new CustomReactiveMongoOperations();

    @Inject
    MongoClient mongoClient;

    @Inject
    ReactiveMongoClient reactiveMongoClient;

    @Test
    public void resolveUsingTwoTenantsImperative() {
        resolveUsingTwoTenantsTest(false);
    }

    @Test
    public void resolveUsingTwoTenantsReactive() {
        resolveUsingTwoTenantsTest(true);
    }

    private void resolveUsingTwoTenantsTest(final boolean isReactive) {
        // arrange
        Person personTenant1 = new Person();
        personTenant1.id = 1L;
        personTenant1.firstname = "Pedro";
        personTenant1.lastname = "Pereira";

        Person personTenant2 = new Person();
        personTenant2.id = 1L;
        personTenant2.firstname = "Tibé";
        personTenant2.lastname = "Venâncio";

        String TENANT_1 = isReactive ? "reactive-sanjoka" : "sanjoka";
        String TENANT_2 = isReactive ? "reactive-mizain" : "mizain";

        persistPerson(isReactive, personTenant1, TENANT_1);
        persistPerson(isReactive, personTenant2, TENANT_2);

        // act
        CustomMongoDatabaseResolver.DATABASE = TENANT_1;
        final Object result1 = findPersonByIdUsingMongoOperations(isReactive, personTenant1.id);

        CustomMongoDatabaseResolver.DATABASE = TENANT_2;
        final Object result2 = findPersonByIdUsingMongoOperations(isReactive, personTenant2.id);

        // assert
        assertPerson(personTenant1, (Person) result1);
        assertPerson(personTenant2, (Person) result2);
    }

    private void persistPerson(final boolean isReactive, final Person person, final String databaseName) {
        final Document document = new Document()
                .append("_id", person.id)
                .append("firstname", person.firstname)
                .append("lastname", person.lastname);

        if (isReactive) {
            reactiveMongoClient.getDatabase(databaseName)
                    .getCollection("persons")
                    .insertOne(document)
                    .await()
                    .indefinitely();
            return;
        }

        mongoClient.getDatabase(databaseName)
                .getCollection("persons")
                .insertOne(document);
    }

    private Person findPersonByIdUsingMongoOperations(final boolean isReactive, final Long id) {
        return isReactive
                ? (Person) REACTIVE_OPERATIONS.findById(Person.class, id).await().indefinitely()
                : (Person) OPERATIONS.findById(Person.class, id);
    }

    private void assertPerson(final Person expected, final Person value) {
        assertNotNull(value);
        assertEquals(expected.id, value.id);
        assertEquals(expected.firstname, value.firstname);
        assertEquals(expected.lastname, value.lastname);
    }

    @SuppressWarnings({ "rawtypes" })
    private static class CustomMongoOperations extends MongoOperations<Object, PanacheUpdate> {

        @Override
        protected Object createQuery(MongoCollection<?> collection, ClientSession session, Bson query, Bson sortDoc) {
            return null;
        }

        @Override
        protected PanacheUpdate createUpdate(MongoCollection collection, Class<?> entityClass, Bson docUpdate) {
            return null;
        }

        @Override
        protected List<?> list(Object queryType) {
            return null;
        }

        @Override
        protected Stream<?> stream(Object queryType) {
            return null;
        }

    }

    @SuppressWarnings({ "rawtypes" })
    private static class CustomReactiveMongoOperations extends ReactiveMongoOperations<Object, PanacheUpdate> {

        @Override
        protected Object createQuery(ReactiveMongoCollection collection, Bson query, Bson sortDoc) {
            return null;
        }

        @Override
        protected PanacheUpdate createUpdate(ReactiveMongoCollection<?> collection, Class<?> entityClass, Bson docUpdate) {
            return null;
        }

        @Override
        protected Uni<?> list(Object query) {
            return null;
        }

        @Override
        protected Multi<?> stream(Object query) {
            return null;
        }

    }

    @MongoEntity(collection = "persons")
    protected static class Person {
        @BsonId
        public Long id;
        public String firstname;
        public String lastname;
    }

    @ApplicationScoped
    private static class CustomMongoDatabaseResolver implements MongoDatabaseResolver {

        public static String DATABASE;

        @Override
        public String resolve() {
            return DATABASE;
        }

    }

}
