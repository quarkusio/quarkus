package io.quarkus.it.mongodb.panache.duplicateids;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;

@Path("/testDuplicateId")
public class TestResource {

    @Inject
    TestDuplicateIdRepository testDuplicateIdRepository;

    @GET
    @Path("imperative/entity")
    public Response TestDuplicateIdEntity() {
        List<TestDuplicateIdEntity> entities = getTestImperativeEntities();

        // insert all
        Assertions.assertEquals(0, TestDuplicateIdEntity.count());
        TestDuplicateIdEntity.persist(entities);
        Assertions.assertEquals(10, TestDuplicateIdEntity.count());

        // Verify that the _id is set
        verifyIdsSet(entities);

        // varargs
        TestDuplicateIdEntity entity11 = new TestDuplicateIdEntity("id11");
        TestDuplicateIdEntity entity12 = new TestDuplicateIdEntity("id11");
        TestDuplicateIdEntity.persist(entity11, entity12);
        Assertions.assertEquals(12, TestDuplicateIdEntity.count());
        entity11.id = "id11Updated";
        entity12.id = "id12Updated";
        TestDuplicateIdEntity.update(entity11, entity12);
        entity11.delete();
        entity12.delete();
        Assertions.assertEquals(10, TestDuplicateIdEntity.count());
        TestDuplicateIdEntity.persistOrUpdate(entity11, entity12);
        Assertions.assertEquals(12, TestDuplicateIdEntity.count());
        entity11.id = "id11Updated";
        entity12.id = "id12Updated";
        TestDuplicateIdEntity.persistOrUpdate(entity11, entity12);
        entity11.delete();
        entity12.delete();
        Assertions.assertEquals(10, TestDuplicateIdEntity.count());

        return Response.ok().build();
    }

    private void verifyIdsSet(List<TestDuplicateIdEntity> entities) {
        for (TestDuplicateIdEntity testDuplicateIdEntity : entities) {

            // This is the key...must not be null.  Field is _id in Mongo.
            Assertions.assertNotNull(testDuplicateIdEntity.objectId);
            Assertions.assertNotNull(testDuplicateIdEntity.id);
        }
    }

    @GET
    @Path("imperative/repository")
    public Response testImperativeRepository() {
        List<TestDuplicateIdEntity> entities = getTestImperativeEntities();

        // insert all
        Assertions.assertEquals(0, testDuplicateIdRepository.count());
        testDuplicateIdRepository.persist(entities);
        Assertions.assertEquals(10, testDuplicateIdRepository.count());

        // Verify that the _id is set
        verifyIdsSet(entities);

        // varargs
        TestDuplicateIdEntity entity11 = new TestDuplicateIdEntity("id11");
        TestDuplicateIdEntity entity12 = new TestDuplicateIdEntity("id12");
        testDuplicateIdRepository.persist(entity11, entity12);
        Assertions.assertEquals(12, testDuplicateIdRepository.count());
        entity11.id = "id11Updated";
        entity12.id = "id12Updated";
        testDuplicateIdRepository.update(entity11, entity12);
        entity11.delete();
        entity12.delete();
        Assertions.assertEquals(10, testDuplicateIdRepository.count());
        testDuplicateIdRepository.persistOrUpdate(entity11, entity12);
        Assertions.assertEquals(12, testDuplicateIdRepository.count());
        entity11.id = "id11Updated";
        entity12.id = "id12Updated";
        testDuplicateIdRepository.persistOrUpdate(entity11, entity12);
        entity11.delete();
        entity12.delete();
        Assertions.assertEquals(10, testDuplicateIdRepository.count());

        return Response.ok().build();
    }

    private List<TestDuplicateIdEntity> getTestImperativeEntities() {
        List<TestDuplicateIdEntity> entities = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            entities.add(new TestDuplicateIdEntity("id" + i));
        }
        return entities;
    }
}
