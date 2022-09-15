package io.quarkus.it.mongodb.panache.test;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.bson.Document;
import org.junit.jupiter.api.Assertions;

import com.mongodb.client.model.Collation;
import com.mongodb.client.model.CollationStrength;

import io.quarkus.mongodb.panache.PanacheQuery;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;

@Path("/test")
public class TestResource {

    @Inject
    TestImperativeRepository testImperativeRepository;
    @Inject
    TestReactiveRepository testReactiveRepository;

    @GET
    @Path("imperative/entity")
    public Response testImperativeEntity() {
        List<TestImperativeEntity> entities = getTestImperativeEntities();

        // insert all
        Assertions.assertEquals(0, TestImperativeEntity.count());
        TestImperativeEntity.persist(entities);
        Assertions.assertEquals(10, TestImperativeEntity.count());

        // varargs
        TestImperativeEntity entity11 = new TestImperativeEntity("title11", "category", "desc");
        TestImperativeEntity entity12 = new TestImperativeEntity("title11", "category", "desc");
        TestImperativeEntity.persist(entity11, entity12);
        Assertions.assertEquals(12, TestImperativeEntity.count());
        entity11.category = "categoryUpdated";
        entity12.category = "categoryUpdated";
        TestImperativeEntity.update(entity11, entity12);
        entity11.delete();
        entity12.delete();
        TestImperativeEntity.persistOrUpdate(entity11, entity12);
        Assertions.assertEquals(12, TestImperativeEntity.count());
        entity11.category = "categoryUpdated";
        entity12.category = "categoryUpdated";
        TestImperativeEntity.persistOrUpdate(entity11, entity12);
        entity11.delete();
        entity12.delete();
        Assertions.assertEquals(10, TestImperativeEntity.count());

        // paginate
        testImperativePagination(TestImperativeEntity.findAll());

        // range
        testImperativeRange(TestImperativeEntity.findAll());

        // query
        Assertions.assertEquals(5, TestImperativeEntity.list("category", "category0").size());
        Assertions.assertEquals(5, TestImperativeEntity.list("category = ?1", "category0").size());
        Assertions.assertEquals(5, TestImperativeEntity.list("category = :category",
                Parameters.with("category", "category1")).size());
        Assertions.assertEquals(5, TestImperativeEntity.list("{'category' : ?1}", "category0").size());
        Assertions.assertEquals(5, TestImperativeEntity.list("{'category' : :category}",
                Parameters.with("category", "category1")).size());
        Document listQuery = new Document().append("category", "category1");
        Assertions.assertEquals(5, TestImperativeEntity.list(listQuery).size());
        Assertions.assertEquals(0, TestImperativeEntity.list("category", (Object) null).size());
        Assertions.assertEquals(0, TestImperativeEntity.list("category = :category",
                Parameters.with("category", null)).size());

        // regex
        TestImperativeEntity entityWithUpperCase = new TestImperativeEntity("title11", "upperCaseCategory", "desc");
        entityWithUpperCase.persist();
        Assertions.assertEquals(1, TestImperativeEntity.list("category like ?1", "upperCase.*").size());
        Assertions.assertEquals(1, TestImperativeEntity.list("category like ?1", "/uppercase.*/i").size());
        entityWithUpperCase.delete();

        // sort
        TestImperativeEntity entityA = new TestImperativeEntity("aaa", "aaa", "aaa");
        entityA.persist();
        TestImperativeEntity entityZ = new TestImperativeEntity("zzz", "zzz", "zzz");
        entityZ.persistOrUpdate();
        TestImperativeEntity result = TestImperativeEntity.<TestImperativeEntity> listAll(Sort.ascending("title")).get(0);
        Assertions.assertEquals("aaa", result.title);
        result = TestImperativeEntity.<TestImperativeEntity> listAll(Sort.descending("title")).get(0);
        Assertions.assertEquals("zzz", result.title);
        entityA.delete();
        entityZ.delete();

        // collation
        TestImperativeEntity entityALower = new TestImperativeEntity("aaa", "aaa", "aaa");
        entityALower.persist();
        TestImperativeEntity entityAUpper = new TestImperativeEntity("AAA", "AAA", "AAA");
        entityAUpper.persist();
        TestImperativeEntity entityB = new TestImperativeEntity("BBB", "BBB", "BBB");
        entityB.persistOrUpdate();
        List<TestImperativeEntity> results = TestImperativeEntity.<TestImperativeEntity> listAll(Sort.ascending("title"));
        Assertions.assertEquals("AAA", results.get(0).title);
        Assertions.assertEquals("BBB", results.get(1).title);
        Assertions.assertEquals("aaa", results.get(2).title);
        Collation collation = Collation.builder().caseLevel(true).collationStrength(CollationStrength.PRIMARY).locale("fr")
                .build();
        results = TestImperativeEntity.<TestImperativeEntity> findAll(Sort.ascending("title")).withCollation(collation).list();
        Assertions.assertEquals("aaa", results.get(0).title);
        Assertions.assertEquals("AAA", results.get(1).title);
        Assertions.assertEquals("BBB", results.get(2).title);

        //count with collation
        collation = Collation.builder()
                .locale("en")
                .collationStrength(CollationStrength.SECONDARY)
                .build();
        Assertions.assertEquals(2, TestImperativeEntity.find("{'title' : ?1}", "aaa").withCollation(collation).count());
        Assertions.assertEquals(2, TestImperativeEntity.find("{'title' : ?1}", "AAA").withCollation(collation).count());
        Assertions.assertEquals(1, TestImperativeEntity.find("{'title' : ?1}", "bbb").withCollation(collation).count());
        Assertions.assertEquals(1, TestImperativeEntity.find("{'title' : ?1}", "BBB").withCollation(collation).count());
        entityAUpper.delete();
        entityALower.delete();
        entityB.delete();

        // count
        Assertions.assertEquals(5, TestImperativeEntity.count("category", "category0"));
        Assertions.assertEquals(5, TestImperativeEntity.count("category = ?1", "category0"));
        Assertions.assertEquals(5, TestImperativeEntity.count("category = :category",
                Parameters.with("category", "category1")));
        Assertions.assertEquals(5, TestImperativeEntity.count("{'category' : ?1}", "category0"));
        Assertions.assertEquals(5, TestImperativeEntity.count("{'category' : :category}",
                Parameters.with("category", "category1")));
        Document countQuery = new Document().append("category", "category1");
        Assertions.assertEquals(5, TestImperativeEntity.count(countQuery));

        // update
        List<TestImperativeEntity> list = TestImperativeEntity.list("category = ?1", "category0");
        Assertions.assertEquals(5, list.size());
        for (TestImperativeEntity entity : list) {
            entity.category = "newCategory";
        }
        TestImperativeEntity.update(list);
        TestImperativeEntity.update(list.stream());
        TestImperativeEntity.persistOrUpdate(list);
        long updated = TestImperativeEntity.update("category", "newCategory2").where("category", "newCategory");
        Assertions.assertEquals(5, updated);
        updated = TestImperativeEntity.update("category = ?1", "newCategory").where("category = ?1", "newCategory2");
        Assertions.assertEquals(5, updated);
        updated = TestImperativeEntity.update("{'category' : ?1}", "newCategory2").where("{'category' : ?1}", "newCategory");
        Assertions.assertEquals(5, updated);
        updated = TestImperativeEntity.update("category = :category", Parameters.with("category", "newCategory"))
                .where("category = :category", Parameters.with("category", "newCategory2"));
        Assertions.assertEquals(5, updated);
        updated = TestImperativeEntity.update("{'category' : :category}", Parameters.with("category", "newCategory2"))
                .where("{'category' : :category}", Parameters.with("category", "newCategory"));
        Assertions.assertEquals(5, updated);
        updated = TestImperativeEntity.update("{'$set': {'category' : :category}}", Parameters.with("category", "newCategory3"))
                .where("{'category' : :category}", Parameters.with("category", "newCategory2"));
        Assertions.assertEquals(5, updated);
        Assertions.assertEquals(5, TestImperativeEntity.count("category = ?1", "newCategory3"));
        updated = TestImperativeEntity.update("newField", "newValue").all();
        Assertions.assertEquals(10, updated);
        updated = TestImperativeEntity.update("{'$inc': {'cpt': 1}}").all();
        Assertions.assertEquals(10, updated);
        updated = TestImperativeEntity.update(new Document("$inc", new Document("cpt", 1)))
                .where(new Document("category", "newCategory3"));
        Assertions.assertEquals(5, updated);
        Assertions.assertEquals(5, TestImperativeEntity.count("cpt = ?1", 3));

        // delete
        TestImperativeEntity.delete("category = ?1", "newCategory3");
        TestImperativeEntity.delete("{'category' : ?1}", "category1");
        Assertions.assertEquals(0, TestImperativeEntity.count());
        TestImperativeEntity.persist(entities.stream());
        TestImperativeEntity.delete("category = :category", Parameters.with("category", "category0"));
        TestImperativeEntity.delete("{'category' : :category}", Parameters.with("category", "category1"));
        Assertions.assertEquals(0, TestImperativeEntity.count());
        TestImperativeEntity.persistOrUpdate(entities.stream());
        TestImperativeEntity.delete("category", "category0");
        TestImperativeEntity.delete("category", "category1");
        Assertions.assertEquals(0, TestImperativeEntity.count());

        return Response.ok().build();
    }

    @GET
    @Path("imperative/repository")
    public Response testImperativeRepository() {
        List<TestImperativeEntity> entities = getTestImperativeEntities();

        // insert all
        Assertions.assertEquals(0, testImperativeRepository.count());
        testImperativeRepository.persist(entities);
        Assertions.assertEquals(10, testImperativeRepository.count());

        // varargs
        TestImperativeEntity entity11 = new TestImperativeEntity("title11", "category", "desc");
        TestImperativeEntity entity12 = new TestImperativeEntity("title11", "category", "desc");
        testImperativeRepository.persist(entity11, entity12);
        Assertions.assertEquals(12, testImperativeRepository.count());
        entity11.category = "categoryUpdated";
        entity12.category = "categoryUpdated";
        testImperativeRepository.update(entity11, entity12);
        entity11.delete();
        entity12.delete();
        testImperativeRepository.persistOrUpdate(entity11, entity12);
        Assertions.assertEquals(12, testImperativeRepository.count());
        entity11.category = "categoryUpdated";
        entity12.category = "categoryUpdated";
        testImperativeRepository.persistOrUpdate(entity11, entity12);
        entity11.delete();
        entity12.delete();
        Assertions.assertEquals(10, testImperativeRepository.count());

        // paginate
        testImperativePagination(testImperativeRepository.findAll());

        // range
        testImperativeRange(testImperativeRepository.findAll());

        // query
        Assertions.assertEquals(5, testImperativeRepository.list("category", "category0").size());
        Assertions.assertEquals(5, testImperativeRepository.list("category = ?1", "category0").size());
        Assertions.assertEquals(5, testImperativeRepository.list("category = :category",
                Parameters.with("category", "category1")).size());
        Assertions.assertEquals(5, testImperativeRepository.list("{'category' : ?1}", "category0").size());
        Assertions.assertEquals(5, testImperativeRepository.list("{'category' : :category}",
                Parameters.with("category", "category1")).size());
        Document listQuery = new Document().append("category", "category1");
        Assertions.assertEquals(5, testImperativeRepository.list(listQuery).size());
        Assertions.assertEquals(0, testImperativeRepository.list("category", (Object) null).size());
        Assertions.assertEquals(0, testImperativeRepository.list("category = :category",
                Parameters.with("category", null)).size());

        // regex
        TestImperativeEntity entityWithUpperCase = new TestImperativeEntity("title11", "upperCaseCategory", "desc");
        testImperativeRepository.persist(entityWithUpperCase);
        Assertions.assertEquals(1, testImperativeRepository.list("category like ?1", "upperCase.*").size());
        Assertions.assertEquals(1, testImperativeRepository.list("category like ?1", "/uppercase.*/i").size());
        testImperativeRepository.delete(entityWithUpperCase);

        // sort
        TestImperativeEntity entityA = new TestImperativeEntity("aaa", "aaa", "aaa");
        testImperativeRepository.persist(entityA);
        TestImperativeEntity entityZ = new TestImperativeEntity("zzz", "zzz", "zzz");
        testImperativeRepository.persistOrUpdate(entityZ);
        TestImperativeEntity result = testImperativeRepository.listAll(Sort.ascending("title")).get(0);
        Assertions.assertEquals("aaa", result.title);
        result = testImperativeRepository.listAll(Sort.descending("title")).get(0);
        Assertions.assertEquals("zzz", result.title);
        testImperativeRepository.delete(entityA);
        testImperativeRepository.delete(entityZ);

        // collation
        TestImperativeEntity entityALower = new TestImperativeEntity("aaa", "aaa", "aaa");
        testImperativeRepository.persist(entityALower);
        TestImperativeEntity entityAUpper = new TestImperativeEntity("AAA", "AAA", "AAA");
        testImperativeRepository.persist(entityAUpper);
        TestImperativeEntity entityB = new TestImperativeEntity("BBB", "BBB", "BBB");
        testImperativeRepository.persistOrUpdate(entityB);
        List<TestImperativeEntity> results = testImperativeRepository.<TestImperativeEntity> listAll(Sort.ascending("title"));
        Assertions.assertEquals("AAA", results.get(0).title);
        Assertions.assertEquals("BBB", results.get(1).title);
        Assertions.assertEquals("aaa", results.get(2).title);
        Collation collation = Collation.builder().caseLevel(true).collationStrength(CollationStrength.PRIMARY).locale("fr")
                .build();
        results = testImperativeRepository.<TestImperativeEntity> findAll(Sort.ascending("title")).withCollation(collation)
                .list();
        Assertions.assertEquals("aaa", results.get(0).title);
        Assertions.assertEquals("AAA", results.get(1).title);
        Assertions.assertEquals("BBB", results.get(2).title);

        //count with collation
        collation = Collation.builder()
                .locale("en")
                .collationStrength(CollationStrength.SECONDARY)
                .build();

        Assertions.assertEquals(2, testImperativeRepository.find("{'title' : ?1}", "aaa").withCollation(collation).count());
        Assertions.assertEquals(2, testImperativeRepository.find("{'title' : ?1}", "AAA").withCollation(collation).count());
        Assertions.assertEquals(1, testImperativeRepository.find("{'title' : ?1}", "bbb").withCollation(collation).count());
        Assertions.assertEquals(1, testImperativeRepository.find("{'title' : ?1}", "BBB").withCollation(collation).count());
        testImperativeRepository.delete(entityALower);
        testImperativeRepository.delete(entityAUpper);
        testImperativeRepository.delete(entityB);

        // count
        Assertions.assertEquals(5, testImperativeRepository.count("category", "category0"));
        Assertions.assertEquals(5, testImperativeRepository.count("category = ?1", "category0"));
        Assertions.assertEquals(5, testImperativeRepository.count("category = :category",
                Parameters.with("category", "category1")));
        Assertions.assertEquals(5, testImperativeRepository.count("{'category' : ?1}", "category0"));
        Assertions.assertEquals(5, testImperativeRepository.count("{'category' : :category}",
                Parameters.with("category", "category1")));
        Document countQuery = new Document().append("category", "category1");
        Assertions.assertEquals(5, testImperativeRepository.count(countQuery));

        // update
        List<TestImperativeEntity> list = testImperativeRepository.list("category = ?1", "category0");
        Assertions.assertEquals(5, list.size());
        for (TestImperativeEntity entity : list) {
            entity.category = "newCategory";
        }
        testImperativeRepository.update(list);
        testImperativeRepository.update(list.stream());
        testImperativeRepository.persistOrUpdate(list);
        long updated = testImperativeRepository.update("category", "newCategory2").where("category", "newCategory");
        Assertions.assertEquals(5, updated);
        updated = testImperativeRepository.update("category = ?1", "newCategory").where("category = ?1", "newCategory2");
        Assertions.assertEquals(5, updated);
        updated = testImperativeRepository.update("{'category' : ?1}", "newCategory2").where("{'category' : ?1}",
                "newCategory");
        Assertions.assertEquals(5, updated);
        updated = testImperativeRepository.update("category = :category", Parameters.with("category", "newCategory"))
                .where("category = :category", Parameters.with("category", "newCategory2"));
        Assertions.assertEquals(5, updated);
        updated = testImperativeRepository.update("{'category' : :category}", Parameters.with("category", "newCategory2"))
                .where("{'category' : :category}", Parameters.with("category", "newCategory"));
        Assertions.assertEquals(5, updated);
        updated = testImperativeRepository
                .update("{'$set': {'category' : :category}}", Parameters.with("category", "newCategory3"))
                .where("{'category' : :category}", Parameters.with("category", "newCategory2"));
        Assertions.assertEquals(5, updated);
        Assertions.assertEquals(5, testImperativeRepository.count("category = ?1", "newCategory3"));
        updated = testImperativeRepository.update("newField", "newValue").all();
        Assertions.assertEquals(10, updated);
        updated = testImperativeRepository.update("{'$inc': {'cpt': 1}}").all();
        Assertions.assertEquals(10, updated);
        updated = testImperativeRepository.update(new Document("$inc", new Document("cpt", 1)))
                .where(new Document("category", "newCategory3"));
        Assertions.assertEquals(5, updated);
        Assertions.assertEquals(5, testImperativeRepository.count("cpt = ?1", 3));

        // delete
        testImperativeRepository.delete("category = ?1", "newCategory3");
        testImperativeRepository.delete("{'category' : ?1}", "category1");
        Assertions.assertEquals(0, testImperativeRepository.count());
        testImperativeRepository.persist(entities.stream());
        testImperativeRepository.delete("category = :category", Parameters.with("category", "category0"));
        testImperativeRepository.delete("{'category' : :category}", Parameters.with("category", "category1"));
        Assertions.assertEquals(0, testImperativeRepository.count());
        testImperativeRepository.persistOrUpdate(entities.stream());
        testImperativeRepository.delete("category", "category0");
        testImperativeRepository.delete("category", "category1");
        Assertions.assertEquals(0, testImperativeRepository.count());

        return Response.ok().build();
    }

    private List<TestImperativeEntity> getTestImperativeEntities() {
        List<TestImperativeEntity> entities = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            entities.add(new TestImperativeEntity("title" + i,
                    "category" + i % 2,
                    "description" + i));
        }
        return entities;
    }

    private void testImperativePagination(PanacheQuery<TestImperativeEntity> query) {
        query.page(0, 4);
        Assertions.assertEquals(3, query.pageCount());
        List<TestImperativeEntity> page = query.list();
        Assertions.assertEquals(4, page.size());
        Assertions.assertTrue(query.hasNextPage());
        Assertions.assertFalse(query.hasPreviousPage());
        query.nextPage();
        page = query.list();
        Assertions.assertEquals(4, page.size());
        Assertions.assertTrue(query.hasNextPage());
        Assertions.assertTrue(query.hasPreviousPage());
        query.lastPage();
        page = query.list();
        Assertions.assertEquals(2, page.size());
        Assertions.assertFalse(query.hasNextPage());
        Assertions.assertTrue(query.hasPreviousPage());
        query.firstPage();
        page = query.list();
        Assertions.assertEquals(4, page.size());
        Assertions.assertTrue(query.hasNextPage());
        Assertions.assertFalse(query.hasPreviousPage());
        query.page(Page.of(1, 5));
        Assertions.assertEquals(2, query.pageCount());
        page = query.list();
        Assertions.assertEquals(5, page.size());
        Assertions.assertFalse(query.hasNextPage());
        Assertions.assertTrue(query.hasPreviousPage());

        // mix page with range
        page = query.page(0, 3).range(0, 1).list();
        Assertions.assertEquals(2, page.size());
    }

    private void testImperativeRange(PanacheQuery<TestImperativeEntity> query) {
        query.range(0, 3);
        List<TestImperativeEntity> range = query.list();
        Assertions.assertEquals(4, range.size());
        range = query.range(4, 7).list();
        Assertions.assertEquals(4, range.size());
        range = query.range(8, 12).list();
        Assertions.assertEquals(2, range.size());
        range = query.range(10, 12).list();
        Assertions.assertEquals(0, range.size());

        // when using range, we cannot use any of the page related operations
        Assertions.assertThrows(UnsupportedOperationException.class, () -> query.range(0, 2).nextPage());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> query.range(0, 2).previousPage());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> query.range(0, 2).pageCount());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> query.range(0, 2).lastPage());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> query.range(0, 2).firstPage());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> query.range(0, 2).hasPreviousPage());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> query.range(0, 2).hasNextPage());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> query.range(0, 2).page());

        // but this is valid to switch from range to page
        range = query.range(0, 2).page(0, 3).list();
        Assertions.assertEquals(3, range.size());
    }

    @GET
    @Path("reactive/entity")
    public Response testReactiveEntity() {
        List<TestReactiveEntity> entities = getTestReactiveEntities();

        // insert all
        Assertions.assertEquals(0, TestReactiveEntity.count().await().indefinitely());
        TestReactiveEntity.persist(entities).await().indefinitely();
        Assertions.assertEquals(10, TestReactiveEntity.count().await().indefinitely());

        // varargs
        TestReactiveEntity entity11 = new TestReactiveEntity("title11", "category", "desc");
        TestReactiveEntity entity12 = new TestReactiveEntity("title11", "category", "desc");
        TestReactiveEntity.persist(entity11, entity12).await().indefinitely();
        Assertions.assertEquals(12, TestReactiveEntity.count().await().indefinitely());
        entity11.category = "categoryUpdated";
        entity12.category = "categoryUpdated";
        TestReactiveEntity.update(entity11, entity12).await().indefinitely();
        entity11.delete().await().indefinitely();
        entity12.delete().await().indefinitely();
        TestReactiveEntity.persistOrUpdate(entity11, entity12).await().indefinitely();
        Assertions.assertEquals(12, TestReactiveEntity.count().await().indefinitely());
        entity11.category = "categoryUpdated";
        entity12.category = "categoryUpdated";
        TestReactiveEntity.persistOrUpdate(entity11, entity12).await().indefinitely();
        entity11.delete().await().indefinitely();
        entity12.delete().await().indefinitely();
        Assertions.assertEquals(10, TestReactiveEntity.count().await().indefinitely());

        // paginate
        testReactivePagination(TestReactiveEntity.findAll());

        // range
        testReactiveRange(TestReactiveEntity.findAll());

        // query
        Assertions.assertEquals(5,
                TestReactiveEntity.list("category", "category0").await().indefinitely().size());
        Assertions.assertEquals(5,
                TestReactiveEntity.list("category = ?1", "category0").await().indefinitely().size());
        Assertions.assertEquals(5, TestReactiveEntity.list("category = :category",
                Parameters.with("category", "category1")).await().indefinitely().size());
        Assertions.assertEquals(5,
                TestReactiveEntity.list("{'category' : ?1}", "category0").await().indefinitely().size());
        Assertions.assertEquals(5, TestReactiveEntity.list("{'category' : :category}",
                Parameters.with("category", "category1")).await().indefinitely().size());
        Document listQuery = new Document().append("category", "category1");
        Assertions.assertEquals(5, TestReactiveEntity.list(listQuery).await().indefinitely().size());
        Assertions.assertEquals(0, TestReactiveEntity.list("category", (Object) null).await().indefinitely().size());
        Assertions.assertEquals(0, TestReactiveEntity.list("category = :category",
                Parameters.with("category", null)).await().indefinitely().size());

        // regex
        TestReactiveEntity entityWithUpperCase = new TestReactiveEntity("title11", "upperCaseCategory", "desc");
        entityWithUpperCase.persist().await().indefinitely();
        Assertions.assertEquals(1, TestReactiveEntity.list("category like ?1", "upperCase.*")
                .await().indefinitely().size());
        Assertions.assertEquals(1, TestReactiveEntity.list("category like ?1", "/uppercase.*/i")
                .await().indefinitely().size());
        entityWithUpperCase.delete().await().indefinitely();

        // sort
        TestReactiveEntity entityA = new TestReactiveEntity("aaa", "aaa", "aaa");
        entityA.persist().await().indefinitely();
        TestReactiveEntity entityZ = new TestReactiveEntity("zzz", "zzz", "zzz");
        entityZ.persistOrUpdate().await().indefinitely();
        TestReactiveEntity result = TestReactiveEntity.<TestReactiveEntity> listAll(Sort.ascending("title")).await()
                .indefinitely().get(0);
        Assertions.assertEquals("aaa", result.title);
        result = TestReactiveEntity.<TestReactiveEntity> listAll(Sort.descending("title")).await().indefinitely().get(0);
        Assertions.assertEquals("zzz", result.title);
        entityA.delete().await().indefinitely();
        entityZ.delete().await().indefinitely();

        // collation
        TestReactiveEntity entityALower = new TestReactiveEntity("aaa", "aaa", "aaa");
        entityALower.persist().await().indefinitely();
        TestReactiveEntity entityAUpper = new TestReactiveEntity("AAA", "AAA", "AAA");
        entityAUpper.persist().await().indefinitely();
        TestReactiveEntity entityB = new TestReactiveEntity("BBB", "BBB", "BBB");
        entityB.persistOrUpdate().await().indefinitely();
        List<TestReactiveEntity> results = TestReactiveEntity.<TestReactiveEntity> listAll(Sort.ascending("title")).await()
                .indefinitely();
        Assertions.assertEquals("AAA", results.get(0).title);
        Assertions.assertEquals("BBB", results.get(1).title);
        Assertions.assertEquals("aaa", results.get(2).title);
        Collation collation = Collation.builder().caseLevel(true).collationStrength(CollationStrength.PRIMARY).locale("fr")
                .build();
        results = TestReactiveEntity.<TestReactiveEntity> findAll(Sort.ascending("title")).withCollation(collation).list()
                .await().indefinitely();
        Assertions.assertEquals("aaa", results.get(0).title);
        Assertions.assertEquals("AAA", results.get(1).title);
        Assertions.assertEquals("BBB", results.get(2).title);

        //count with collation
        collation = Collation.builder()
                .locale("en")
                .collationStrength(CollationStrength.SECONDARY)
                .build();
        Assertions.assertEquals(2, TestReactiveEntity.find("{'title' : ?1}", "aaa").withCollation(collation).count()
                .await().indefinitely());
        Assertions.assertEquals(2, TestReactiveEntity.find("{'title' : ?1}", "AAA").withCollation(collation).count()
                .await().indefinitely());
        Assertions.assertEquals(1, TestReactiveEntity.find("{'title' : ?1}", "bbb").withCollation(collation).count()
                .await().indefinitely());
        Assertions.assertEquals(1, TestReactiveEntity.find("{'title' : ?1}", "BBB").withCollation(collation).count()
                .await().indefinitely());
        entityAUpper.delete().await().indefinitely();
        entityALower.delete().await().indefinitely();
        entityB.delete().await().indefinitely();

        // count
        Assertions.assertEquals(5, TestReactiveEntity.count("category", "category0").await().indefinitely());
        Assertions.assertEquals(5, TestReactiveEntity.count("category = ?1", "category0").await().indefinitely());
        Assertions.assertEquals(5, TestReactiveEntity.count("category = :category",
                Parameters.with("category", "category1")).await().indefinitely());
        Assertions.assertEquals(5, TestReactiveEntity.count("{'category' : ?1}", "category0").await().indefinitely());
        Assertions.assertEquals(5, TestReactiveEntity.count("{'category' : :category}",
                Parameters.with("category", "category1")).await().indefinitely());
        Document countQuery = new Document().append("category", "category1");
        Assertions.assertEquals(5, TestReactiveEntity.count(countQuery).await().indefinitely());

        // update
        List<TestReactiveEntity> list = TestReactiveEntity.<TestReactiveEntity> list("category = ?1", "category0").await()
                .indefinitely();
        Assertions.assertEquals(5, list.size());
        for (TestReactiveEntity entity : list) {
            entity.category = "newCategory";
        }
        TestReactiveEntity.update(list).await().indefinitely();
        TestReactiveEntity.update(list.stream()).await().indefinitely();
        TestReactiveEntity.persistOrUpdate(list).await().indefinitely();
        long updated = TestReactiveEntity.update("category", "newCategory2").where("category", "newCategory").await()
                .indefinitely();
        Assertions.assertEquals(5, updated);
        updated = TestReactiveEntity.update("category = ?1", "newCategory").where("category = ?1", "newCategory2").await()
                .indefinitely();
        Assertions.assertEquals(5, updated);
        updated = TestReactiveEntity.update("{'category' : ?1}", "newCategory2").where("{'category' : ?1}", "newCategory")
                .await().indefinitely();
        Assertions.assertEquals(5, updated);
        updated = TestReactiveEntity.update("category = :category", Parameters.with("category", "newCategory"))
                .where("category = :category", Parameters.with("category", "newCategory2")).await().indefinitely();
        Assertions.assertEquals(5, updated);
        updated = TestReactiveEntity.update("{'category' : :category}", Parameters.with("category", "newCategory2"))
                .where("{'category' : :category}", Parameters.with("category", "newCategory")).await().indefinitely();
        Assertions.assertEquals(5, updated);
        updated = TestReactiveEntity.update("{'$set': {'category' : :category}}", Parameters.with("category", "newCategory3"))
                .where("{'category' : :category}", Parameters.with("category", "newCategory2")).await().indefinitely();
        Assertions.assertEquals(5, updated);
        Assertions.assertEquals(5, TestReactiveEntity.count("category = ?1", "newCategory3").await().indefinitely());
        updated = TestReactiveEntity.update("newField", "newValue").all().await().indefinitely();
        Assertions.assertEquals(10, updated);
        updated = TestReactiveEntity.update("{'$inc': {'cpt': 1}}").all().await().indefinitely();
        Assertions.assertEquals(10, updated);
        updated = TestReactiveEntity.update(new Document("$inc", new Document("cpt", 1)))
                .where(new Document("category", "newCategory3")).await().indefinitely();
        Assertions.assertEquals(5, updated);
        Assertions.assertEquals(5, TestReactiveEntity.count("cpt = ?1", 3).await().indefinitely());

        // delete
        TestReactiveEntity.delete("category = ?1", "newCategory3").await().indefinitely();
        TestReactiveEntity.delete("{'category' : ?1}", "category1").await().indefinitely();
        Assertions.assertEquals(0, TestReactiveEntity.count().await().indefinitely());
        TestReactiveEntity.persist(entities.stream()).await().indefinitely();
        TestReactiveEntity.delete("category = :category", Parameters.with("category", "category0")).await().indefinitely();
        TestReactiveEntity.delete("{'category' : :category}", Parameters.with("category", "category1")).await().indefinitely();
        Assertions.assertEquals(0, TestReactiveEntity.count().await().indefinitely());
        TestReactiveEntity.persistOrUpdate(entities.stream()).await().indefinitely();
        TestReactiveEntity.delete("category", "category0").await().indefinitely();
        TestReactiveEntity.delete("category", "category1").await().indefinitely();
        Assertions.assertEquals(0, TestReactiveEntity.count().await().indefinitely());

        return Response.ok().build();
    }

    @GET
    @Path("reactive/repository")
    public Response testReactiveRepository() {
        List<TestReactiveEntity> entities = getTestReactiveEntities();

        // insert all
        Assertions.assertEquals(0, testReactiveRepository.count().await().indefinitely());
        testReactiveRepository.persist(entities).await().indefinitely();
        Assertions.assertEquals(10, testReactiveRepository.count().await().indefinitely());

        // varargs
        TestReactiveEntity entity11 = new TestReactiveEntity("title11", "category", "desc");
        TestReactiveEntity entity12 = new TestReactiveEntity("title11", "category", "desc");
        testReactiveRepository.persist(entity11, entity12).await().indefinitely();
        Assertions.assertEquals(12, testReactiveRepository.count().await().indefinitely());
        entity11.category = "categoryUpdated";
        entity12.category = "categoryUpdated";
        testReactiveRepository.update(entity11, entity12).await().indefinitely();
        entity11.delete().await().indefinitely();
        entity12.delete().await().indefinitely();
        testReactiveRepository.persistOrUpdate(entity11, entity12).await().indefinitely();
        Assertions.assertEquals(12, testReactiveRepository.count().await().indefinitely());
        entity11.category = "categoryUpdated";
        entity12.category = "categoryUpdated";
        testReactiveRepository.persistOrUpdate(entity11, entity12).await().indefinitely();
        entity11.delete().await().indefinitely();
        entity12.delete().await().indefinitely();
        Assertions.assertEquals(10, testReactiveRepository.count().await().indefinitely());

        // paginate
        testReactivePagination(testReactiveRepository.findAll());

        // range
        testReactiveRange(testReactiveRepository.findAll());

        // query
        Assertions.assertEquals(5,
                testReactiveRepository.list("category", "category0").await().indefinitely().size());
        Assertions.assertEquals(5,
                testReactiveRepository.list("category = ?1", "category0").await().indefinitely().size());
        Assertions.assertEquals(5, testReactiveRepository.list("category = :category",
                Parameters.with("category", "category1")).await().indefinitely().size());
        Assertions.assertEquals(5,
                testReactiveRepository.list("{'category' : ?1}", "category0").await().indefinitely().size());
        Assertions.assertEquals(5, testReactiveRepository.list("{'category' : :category}",
                Parameters.with("category", "category1")).await().indefinitely().size());
        Document listQuery = new Document().append("category", "category1");
        Assertions.assertEquals(5, testReactiveRepository.list(listQuery).await().indefinitely().size());
        Assertions.assertEquals(0, testReactiveRepository.list("category", (Object) null).await().indefinitely().size());
        Assertions.assertEquals(0, testReactiveRepository.list("category = :category",
                Parameters.with("category", null)).await().indefinitely().size());

        // regex
        TestReactiveEntity entityWithUpperCase = new TestReactiveEntity("title11", "upperCaseCategory", "desc");
        testReactiveRepository.persist(entityWithUpperCase).await().indefinitely();
        Assertions.assertEquals(1, testReactiveRepository.list("category like ?1", "upperCase.*")
                .await().indefinitely().size());
        Assertions.assertEquals(1, testReactiveRepository.list("category like ?1", "/uppercase.*/i")
                .await().indefinitely().size());
        testReactiveRepository.delete(entityWithUpperCase).await().indefinitely();

        // sort
        TestReactiveEntity entityA = new TestReactiveEntity("aaa", "aaa", "aaa");
        testReactiveRepository.persist(entityA).await().indefinitely();
        TestReactiveEntity entityZ = new TestReactiveEntity("zzz", "zzz", "zzz");
        testReactiveRepository.persistOrUpdate(entityZ).await().indefinitely();
        TestReactiveEntity result = testReactiveRepository.listAll(Sort.ascending("title")).await().indefinitely().get(0);
        Assertions.assertEquals("aaa", result.title);
        result = testReactiveRepository.listAll(Sort.descending("title")).await().indefinitely().get(0);
        Assertions.assertEquals("zzz", result.title);
        testReactiveRepository.delete(entityA).await().indefinitely();
        testReactiveRepository.delete(entityZ).await().indefinitely();

        // collation
        TestReactiveEntity entityALower = new TestReactiveEntity("aaa", "aaa", "aaa");
        testReactiveRepository.persist(entityALower).await().indefinitely();
        TestReactiveEntity entityAUpper = new TestReactiveEntity("AAA", "AAA", "AAA");
        testReactiveRepository.persist(entityAUpper).await().indefinitely();
        TestReactiveEntity entityB = new TestReactiveEntity("BBB", "BBB", "BBB");
        testReactiveRepository.persistOrUpdate(entityB).await().indefinitely();
        List<TestReactiveEntity> results = testReactiveRepository.listAll(Sort.ascending("title")).await().indefinitely();
        Assertions.assertEquals("AAA", results.get(0).title);
        Assertions.assertEquals("BBB", results.get(1).title);
        Assertions.assertEquals("aaa", results.get(2).title);
        Collation collation = Collation.builder().caseLevel(true).collationStrength(CollationStrength.PRIMARY).locale("fr")
                .build();
        results = testReactiveRepository.findAll(Sort.ascending("title")).withCollation(collation).list().await()
                .indefinitely();
        Assertions.assertEquals("aaa", results.get(0).title);
        Assertions.assertEquals("AAA", results.get(1).title);
        Assertions.assertEquals("BBB", results.get(2).title);

        //count with collation
        collation = Collation.builder()
                .locale("en")
                .collationStrength(CollationStrength.SECONDARY)
                .build();
        Assertions.assertEquals(2, testReactiveRepository.find("{'title' : ?1}", "aaa").withCollation(collation).count()
                .await().indefinitely());
        Assertions.assertEquals(2, testReactiveRepository.find("{'title' : ?1}", "AAA").withCollation(collation).count()
                .await().indefinitely());
        Assertions.assertEquals(1, testReactiveRepository.find("{'title' : ?1}", "bbb").withCollation(collation).count()
                .await().indefinitely());
        Assertions.assertEquals(1, testReactiveRepository.find("{'title' : ?1}", "BBB").withCollation(collation).count()
                .await().indefinitely());
        testReactiveRepository.delete(entityALower).await().indefinitely();
        testReactiveRepository.delete(entityAUpper).await().indefinitely();
        testReactiveRepository.delete(entityB).await().indefinitely();

        // count
        Assertions.assertEquals(5,
                testReactiveRepository.count("category", "category0").await().indefinitely());
        Assertions.assertEquals(5,
                testReactiveRepository.count("category = ?1", "category0").await().indefinitely());
        Assertions.assertEquals(5, testReactiveRepository.count("category = :category",
                Parameters.with("category", "category1")).await().indefinitely());
        Assertions.assertEquals(5,
                testReactiveRepository.count("{'category' : ?1}", "category0").await().indefinitely());
        Assertions.assertEquals(5, testReactiveRepository.count("{'category' : :category}",
                Parameters.with("category", "category1")).await().indefinitely());
        Document countQuery = new Document().append("category", "category1");
        Assertions.assertEquals(5, testReactiveRepository.count(countQuery).await().indefinitely());

        // update
        List<TestReactiveEntity> list = testReactiveRepository.list("category = ?1", "category0").await().indefinitely();
        Assertions.assertEquals(5, list.size());
        for (TestReactiveEntity entity : list) {
            entity.category = "newCategory";
        }
        testReactiveRepository.update(list).await().indefinitely();
        testReactiveRepository.update(list.stream()).await().indefinitely();
        testReactiveRepository.persistOrUpdate(list).await().indefinitely();
        long updated = testReactiveRepository.update("category", "newCategory2").where("category", "newCategory").await()
                .indefinitely();
        Assertions.assertEquals(5, updated);
        updated = testReactiveRepository.update("category = ?1", "newCategory").where("category = ?1", "newCategory2").await()
                .indefinitely();
        Assertions.assertEquals(5, updated);
        updated = testReactiveRepository.update("{'category' : ?1}", "newCategory2").where("{'category' : ?1}", "newCategory")
                .await().indefinitely();
        Assertions.assertEquals(5, updated);
        updated = testReactiveRepository.update("category = :category", Parameters.with("category", "newCategory"))
                .where("category = :category", Parameters.with("category", "newCategory2")).await().indefinitely();
        Assertions.assertEquals(5, updated);
        updated = testReactiveRepository.update("{'category' : :category}", Parameters.with("category", "newCategory2"))
                .where("{'category' : :category}", Parameters.with("category", "newCategory")).await().indefinitely();
        Assertions.assertEquals(5, updated);
        updated = testReactiveRepository
                .update("{'$set': {'category' : :category}}", Parameters.with("category", "newCategory3"))
                .where("{'category' : :category}", Parameters.with("category", "newCategory2")).await().indefinitely();
        Assertions.assertEquals(5, updated);
        Assertions.assertEquals(5, testReactiveRepository.count("category = ?1", "newCategory3").await().indefinitely());
        updated = testReactiveRepository.update("newField", "newValue").all().await().indefinitely();
        Assertions.assertEquals(10, updated);
        updated = testReactiveRepository.update("{'$inc': {'cpt': 1}}").all().await().indefinitely();
        Assertions.assertEquals(10, updated);
        updated = testReactiveRepository.update(new Document("$inc", new Document("cpt", 1)))
                .where(new Document("category", "newCategory3")).await().indefinitely();
        Assertions.assertEquals(5, updated);
        Assertions.assertEquals(5, testReactiveRepository.count("cpt = ?1", 3).await().indefinitely());

        // delete
        testReactiveRepository.delete("category = ?1", "newCategory3").await().indefinitely();
        testReactiveRepository.delete("{'category' : ?1}", "category1").await().indefinitely();
        Assertions.assertEquals(0, testReactiveRepository.count().await().indefinitely());
        testReactiveRepository.persist(entities.stream()).await().indefinitely();
        testReactiveRepository.delete("category = :category", Parameters.with("category", "category0")).await().indefinitely();
        testReactiveRepository.delete("{'category' : :category}", Parameters.with("category", "category1")).await()
                .indefinitely();
        Assertions.assertEquals(0, testReactiveRepository.count().await().indefinitely());
        testReactiveRepository.persistOrUpdate(entities.stream()).await().indefinitely();
        testReactiveRepository.delete("category", "category0").await().indefinitely();
        testReactiveRepository.delete("category", "category1").await().indefinitely();
        Assertions.assertEquals(0, testReactiveRepository.count().await().indefinitely());

        return Response.ok().build();
    }

    private List<TestReactiveEntity> getTestReactiveEntities() {
        List<TestReactiveEntity> entities = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            entities.add(new TestReactiveEntity("title" + i,
                    "category" + i % 2,
                    "description" + i));
        }
        return entities;
    }

    private void testReactivePagination(ReactivePanacheQuery<TestReactiveEntity> query) {
        query.page(0, 4);
        Assertions.assertEquals(3, query.pageCount().await().indefinitely());
        List<TestReactiveEntity> page = query.list().await().indefinitely();
        Assertions.assertEquals(4, page.size());
        Assertions.assertTrue(query.hasNextPage().await().indefinitely());
        Assertions.assertFalse(query.hasPreviousPage());
        query.nextPage();
        page = query.list().await().indefinitely();

        Assertions.assertEquals(4, page.size());
        Assertions.assertTrue(query.hasNextPage().await().indefinitely());
        Assertions.assertTrue(query.hasPreviousPage());
        query.lastPage().await().indefinitely();
        page = query.list().await().indefinitely();

        Assertions.assertEquals(2, page.size());
        Assertions.assertFalse(query.hasNextPage().await().indefinitely());
        Assertions.assertTrue(query.hasPreviousPage());
        query.firstPage();
        page = query.list().await().indefinitely();
        Assertions.assertEquals(4, page.size());
        Assertions.assertTrue(query.hasNextPage().await().indefinitely());
        Assertions.assertFalse(query.hasPreviousPage());
        query.page(Page.of(1, 5));
        Assertions.assertEquals(2, query.pageCount().await().indefinitely());
        page = query.list().await().indefinitely();
        Assertions.assertEquals(5, page.size());
        Assertions.assertFalse(query.hasNextPage().await().indefinitely());
        Assertions.assertTrue(query.hasPreviousPage());

        // mix page with range
        page = query.page(0, 3).range(0, 1).list().await().indefinitely();
        Assertions.assertEquals(2, page.size());
    }

    private void testReactiveRange(ReactivePanacheQuery<TestReactiveEntity> query) {
        query.range(0, 3);
        List<TestReactiveEntity> range = query.list().await().indefinitely();
        Assertions.assertEquals(4, range.size());
        range = query.range(4, 7).list().await().indefinitely();
        Assertions.assertEquals(4, range.size());
        range = query.range(8, 12).list().await().indefinitely();
        Assertions.assertEquals(2, range.size());
        range = query.range(10, 12).list().await().indefinitely();
        Assertions.assertEquals(0, range.size());

        // when using range, we cannot use any of the page related operations
        Assertions.assertThrows(UnsupportedOperationException.class, () -> query.range(0, 2).nextPage());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> query.range(0, 2).previousPage());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> query.range(0, 2).pageCount());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> query.range(0, 2).lastPage());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> query.range(0, 2).firstPage());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> query.range(0, 2).hasPreviousPage());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> query.range(0, 2).hasNextPage());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> query.range(0, 2).page());

        // but this is valid to switch from range to page
        range = query.range(0, 2).page(0, 3).list().await().indefinitely();
        Assertions.assertEquals(3, range.size());
    }
}
