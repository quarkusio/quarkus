package io.quarkus.it.mongodb.panache.test

import com.mongodb.client.model.Collation
import com.mongodb.client.model.CollationStrength
import io.quarkus.mongodb.panache.kotlin.PanacheQuery
import io.quarkus.mongodb.panache.kotlin.reactive.ReactivePanacheQuery
import io.quarkus.panache.common.Page
import io.quarkus.panache.common.Parameters
import io.quarkus.panache.common.Sort
import org.bson.Document
import org.junit.jupiter.api.Assertions
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.core.Response

@Path("/test")
class TestResource {
    @Inject
    lateinit var testImperativeRepository: TestImperativeRepository

    @Inject
    lateinit var testReactiveRepository: TestReactiveRepository

    private val testImperativeEntities: List<TestImperativeEntity> by lazy {
        (0..9).map { TestImperativeEntity("title$it", "category" + it % 2, "description$it") }
    }

    @GET
    @Path("imperative/entity")
    fun testImperativeEntity(): Response {
        val entities: List<TestImperativeEntity> = testImperativeEntities

        // insert all
        Assertions.assertEquals(0, TestImperativeEntity.count())
        TestImperativeEntity.persist(entities)
        Assertions.assertEquals(10, TestImperativeEntity.count())

        // varargs
        val entity11 = TestImperativeEntity("title11", "category", "desc")
        val entity12 = TestImperativeEntity("title11", "category", "desc")
        TestImperativeEntity.persist(entity11, entity12)
        Assertions.assertEquals(12, TestImperativeEntity.count())
        entity11.category = "categoryUpdated"
        entity12.category = "categoryUpdated"
        TestImperativeEntity.update(entity11, entity12)
        entity11.delete()
        entity12.delete()
        TestImperativeEntity.persistOrUpdate(entity11, entity12)
        Assertions.assertEquals(12, TestImperativeEntity.count())
        entity11.category = "categoryUpdated"
        entity12.category = "categoryUpdated"
        TestImperativeEntity.persistOrUpdate(entity11, entity12)
        entity11.delete()
        entity12.delete()
        Assertions.assertEquals(10, TestImperativeEntity.count())

        // paginate
        testImperativePagination(TestImperativeEntity.findAll())

        // range
        testImperativeRange(TestImperativeEntity.findAll())

        // query
        Assertions.assertEquals(5, TestImperativeEntity.list("category", "category0").size)
        Assertions.assertEquals(5, TestImperativeEntity.list("category = ?1", "category0").size)
        Assertions.assertEquals(5, TestImperativeEntity.list("category = :category",
                Parameters.with("category", "category1")).size)
        Assertions.assertEquals(5, TestImperativeEntity.list("{'category' : ?1}", "category0").size)
        Assertions.assertEquals(5, TestImperativeEntity.list("{'category' : :category}",
                Parameters.with("category", "category1")).size)
        val listQuery: Document = Document().append("category", "category1")
        Assertions.assertEquals(5, TestImperativeEntity.list(listQuery).size)
        Assertions.assertEquals(0, TestImperativeEntity.list("category", null).size)
        Assertions.assertEquals(0, TestImperativeEntity.list("category = :category",
                Parameters.with("category", null)).size)

        // regex
        val entityWithUpperCase = TestImperativeEntity("title11", "upperCaseCategory", "desc")
        entityWithUpperCase.persist()
        Assertions.assertEquals(1, TestImperativeEntity.list("category like ?1", "upperCase.*").size)
        Assertions.assertEquals(1, TestImperativeEntity.list("category like ?1", "/uppercase.*/i").size)
        entityWithUpperCase.delete()

        // sort
        val entityA = TestImperativeEntity("aaa", "aaa", "aaa")
        entityA.persist()
        val entityZ = TestImperativeEntity("zzz", "zzz", "zzz")
        entityZ.persistOrUpdate()
        var result: TestImperativeEntity = TestImperativeEntity.listAll(Sort.ascending("title"))[0]
        Assertions.assertEquals("aaa", result.title)
        result = TestImperativeEntity.listAll(Sort.descending("title"))[0]
        Assertions.assertEquals("zzz", result.title)
        entityA.delete()
        entityZ.delete()

        // collation
        val entityALower = TestImperativeEntity("aaa", "aaa", "aaa")
        entityALower.persist()
        val entityAUpper = TestImperativeEntity("AAA", "AAA", "AAA")
        entityAUpper.persist()
        val entityB = TestImperativeEntity("BBB", "BBB", "BBB")
        entityB.persistOrUpdate()
        var results: List<TestImperativeEntity> = TestImperativeEntity.listAll(Sort.ascending("title"))
        Assertions.assertEquals("AAA", results[0].title)
        Assertions.assertEquals("BBB", results[1].title)
        Assertions.assertEquals("aaa", results[2].title)
        val collation: Collation = Collation.builder().caseLevel(true).collationStrength(CollationStrength.PRIMARY).locale("fr")
                .build()
        results = TestImperativeEntity.findAll(Sort.ascending("title")).withCollation(collation).list()
        Assertions.assertEquals("aaa", results[0].title)
        Assertions.assertEquals("AAA", results[1].title)
        Assertions.assertEquals("BBB", results[2].title)
        entityAUpper.delete()
        entityALower.delete()
        entityB.delete()

        // count
        Assertions.assertEquals(5, TestImperativeEntity.count("category", "category0"))
        Assertions.assertEquals(5, TestImperativeEntity.count("category = ?1", "category0"))
        Assertions.assertEquals(5, TestImperativeEntity.count("category = :category",
                Parameters.with("category", "category1")))
        Assertions.assertEquals(5, TestImperativeEntity.count("{'category' : ?1}", "category0"))
        Assertions.assertEquals(5, TestImperativeEntity.count("{'category' : :category}",
                Parameters.with("category", "category1")))
        val countQuery: Document = Document().append("category", "category1")
        Assertions.assertEquals(5, TestImperativeEntity.count(countQuery))

        // update
        val list: List<TestImperativeEntity> = TestImperativeEntity.list("category = ?1", "category0")
        Assertions.assertEquals(5, list.size)
        for (entity in list) {
            entity.category = "newCategory"
        }
        TestImperativeEntity.update(list)
        TestImperativeEntity.update(list.stream())
        TestImperativeEntity.persistOrUpdate(list)
        var updated: Long = TestImperativeEntity.update("category", "newCategory2").where("category", "newCategory")
        Assertions.assertEquals(5, updated)
        updated = TestImperativeEntity.update("category = ?1", "newCategory").where("category = ?1", "newCategory2")
        Assertions.assertEquals(5, updated)
        updated = TestImperativeEntity.update("{'category' : ?1}", "newCategory2").where("{'category' : ?1}", "newCategory")
        Assertions.assertEquals(5, updated)
        updated = TestImperativeEntity.update("category = :category", Parameters.with("category", "newCategory"))
                .where("category = :category", Parameters.with("category", "newCategory2"))
        Assertions.assertEquals(5, updated)
        updated = TestImperativeEntity.update("{'category' : :category}", Parameters.with("category", "newCategory2"))
                .where("{'category' : :category}", Parameters.with("category", "newCategory"))
        Assertions.assertEquals(5, updated)
        Assertions.assertEquals(5, TestImperativeEntity.count("category = ?1", "newCategory2"))
        updated = TestImperativeEntity.update("newField", "newValue").all()
        Assertions.assertEquals(10, updated)

        // delete
        TestImperativeEntity.delete("category = ?1", "newCategory2")
        TestImperativeEntity.delete("{'category' : ?1}", "category1")
        Assertions.assertEquals(0, TestImperativeEntity.count())
        TestImperativeEntity.persist(entities.stream())
        TestImperativeEntity.delete("category = :category", Parameters.with("category", "category0"))
        TestImperativeEntity.delete("{'category' : :category}", Parameters.with("category", "category1"))
        Assertions.assertEquals(0, TestImperativeEntity.count())
        TestImperativeEntity.persistOrUpdate(entities.stream())
        TestImperativeEntity.delete("category", "category0")
        TestImperativeEntity.delete("category", "category1")
        Assertions.assertEquals(0, TestImperativeEntity.count())
        return Response.ok().build()
    }

    @GET
    @Path("imperative/repository")
    fun testImperativeRepository(): Response {
        val entities: List<TestImperativeEntity> = testImperativeEntities

        // insert all
        Assertions.assertEquals(0, testImperativeRepository.count())
        testImperativeRepository.persist(entities)
        Assertions.assertEquals(10, testImperativeRepository.count())

        // varargs
        val entity11 = TestImperativeEntity("title11", "category", "desc")
        val entity12 = TestImperativeEntity("title11", "category", "desc")
        testImperativeRepository.persist(entity11, entity12)
        Assertions.assertEquals(12, testImperativeRepository.count())
        entity11.category = "categoryUpdated"
        entity12.category = "categoryUpdated"
        testImperativeRepository.update(entity11, entity12)
        entity11.delete()
        entity12.delete()
        testImperativeRepository.persistOrUpdate(entity11, entity12)
        Assertions.assertEquals(12, testImperativeRepository.count())
        entity11.category = "categoryUpdated"
        entity12.category = "categoryUpdated"
        testImperativeRepository.persistOrUpdate(entity11, entity12)
        entity11.delete()
        entity12.delete()
        Assertions.assertEquals(10, testImperativeRepository.count())

        // paginate
        testImperativePagination(testImperativeRepository.findAll())

        // range
        testImperativeRange(testImperativeRepository.findAll())

        // query
        Assertions.assertEquals(5, testImperativeRepository.list("category", "category0").size)
        Assertions.assertEquals(5, testImperativeRepository.list("category = ?1", "category0").size)
        Assertions.assertEquals(5, testImperativeRepository.list("category = :category",
                Parameters.with("category", "category1")).size)
        Assertions.assertEquals(5, testImperativeRepository.list("{'category' : ?1}", "category0").size)
        Assertions.assertEquals(5, testImperativeRepository.list("{'category' : :category}",
                Parameters.with("category", "category1")).size)
        val listQuery: Document = Document().append("category", "category1")
        Assertions.assertEquals(5, testImperativeRepository.list(listQuery).size)
        Assertions.assertEquals(0, testImperativeRepository.list("category", null).size)
        Assertions.assertEquals(0, testImperativeRepository.list("category = :category",
                Parameters.with("category", null)).size)

        // regex
        val entityWithUpperCase = TestImperativeEntity("title11", "upperCaseCate)gory", "desc")
        testImperativeRepository.persist(entityWithUpperCase)
        Assertions.assertEquals(1, testImperativeRepository.list("category like ?1", "upperCase.*").size)
        Assertions.assertEquals(1, testImperativeRepository.list("category like ?1", "/uppercase.*/i").size)
        testImperativeRepository.delete(entityWithUpperCase)

        // sort
        val entityA = TestImperativeEntity("aaa", "aaa", "aaa")
        testImperativeRepository.persist(entityA)
        val entityZ = TestImperativeEntity("zzz", "zzz", "zzz")
        testImperativeRepository.persistOrUpdate(entityZ)
        var result: TestImperativeEntity = testImperativeRepository.listAll(Sort.ascending("title"))[0]
        Assertions.assertEquals("aaa", result.title)
        result = testImperativeRepository.listAll(Sort.descending("title"))[0]
        Assertions.assertEquals("zzz", result.title)
        testImperativeRepository.delete(entityA)
        testImperativeRepository.delete(entityZ)

        // collation
        val entityALower = TestImperativeEntity("aaa", "aaa", "aaa")
        testImperativeRepository.persist(entityALower)
        val entityAUpper = TestImperativeEntity("AAA", "AAA", "AAA")
        testImperativeRepository.persist(entityAUpper)
        val entityB = TestImperativeEntity("BBB", "BBB", "BBB")
        testImperativeRepository.persistOrUpdate(entityB)
        var results: List<TestImperativeEntity> = testImperativeRepository.listAll(Sort.ascending("title"))
        Assertions.assertEquals("AAA", results[0].title)
        Assertions.assertEquals("BBB", results[1].title)
        Assertions.assertEquals("aaa", results[2].title)
        val collation: Collation = Collation.builder().caseLevel(true).collationStrength(CollationStrength.PRIMARY).locale("fr")
                .build()
        results = testImperativeRepository.findAll(Sort.ascending("title"))
                .withCollation(collation)
                .list()
        Assertions.assertEquals("aaa", results[0].title)
        Assertions.assertEquals("AAA", results[1].title)
        Assertions.assertEquals("BBB", results[2].title)
        testImperativeRepository.delete(entityALower)
        testImperativeRepository.delete(entityAUpper)
        testImperativeRepository.delete(entityB)

        // count
        Assertions.assertEquals(5, testImperativeRepository.count("category", "category0"))
        Assertions.assertEquals(5, testImperativeRepository.count("category = ?1", "category0"))
        Assertions.assertEquals(5, testImperativeRepository.count("category = :category",
                Parameters.with("category", "category1")))
        Assertions.assertEquals(5, testImperativeRepository.count("{'category' : ?1}", "category0"))
        Assertions.assertEquals(5, testImperativeRepository.count("{'category' : :category}",
                Parameters.with("category", "category1")))
        val countQuery: Document = Document().append("category", "category1")
        Assertions.assertEquals(5, testImperativeRepository.count(countQuery))

        // update
        val list: List<TestImperativeEntity> = testImperativeRepository.list("category = ?1", "category0")
        Assertions.assertEquals(5, list.size)
        for (entity in list) {
            entity.category = "newCategory"
        }
        testImperativeRepository.update(list)
        testImperativeRepository.update(list.stream())
        testImperativeRepository.persistOrUpdate(list)
        var updated: Long = testImperativeRepository.update("category", "newCategory2").where("category", "newCategory")
        Assertions.assertEquals(5, updated)
        updated = testImperativeRepository.update("category = ?1", "newCategory").where("category = ?1", "newCategory2")
        Assertions.assertEquals(5, updated)
        updated = testImperativeRepository.update("{'category' : ?1}", "newCategory2").where("{'category' : ?1}",
                "newCategory")
        Assertions.assertEquals(5, updated)
        updated = testImperativeRepository.update("category = :category", Parameters.with("category", "newCategory"))
                .where("category = :category", Parameters.with("category", "newCategory2"))
        Assertions.assertEquals(5, updated)
        updated = testImperativeRepository.update("{'category' : :category}", Parameters.with("category", "newCategory2"))
                .where("{'category' : :category}", Parameters.with("category", "newCategory"))
        Assertions.assertEquals(5, updated)
        Assertions.assertEquals(5, testImperativeRepository.count("category = ?1", "newCategory2"))
        updated = testImperativeRepository.update("newField", "newValue").all()
        Assertions.assertEquals(10, updated)

        // delete
        testImperativeRepository.delete("category = ?1", "newCategory2")
        testImperativeRepository.delete("{'category' : ?1}", "category1")
        Assertions.assertEquals(0, testImperativeRepository.count())
        testImperativeRepository.persist(entities.stream())
        testImperativeRepository.delete("category = :category", Parameters.with("category", "category0"))
        testImperativeRepository.delete("{'category' : :category}", Parameters.with("category", "category1"))
        Assertions.assertEquals(0, testImperativeRepository.count())
        testImperativeRepository.persistOrUpdate(entities.stream())
        testImperativeRepository.delete("category", "category0")
        testImperativeRepository.delete("category", "category1")
        Assertions.assertEquals(0, testImperativeRepository.count())
        return Response.ok().build()
    }

    private fun testImperativePagination(query: PanacheQuery<TestImperativeEntity>) {
        query.page(0, 4)
        Assertions.assertEquals(3, query.pageCount())
        var page: List<TestImperativeEntity> = query.list()
        Assertions.assertEquals(4, page.size)
        Assertions.assertTrue(query.hasNextPage())
        Assertions.assertFalse(query.hasPreviousPage())
        query.nextPage()
        page = query.list()
        Assertions.assertEquals(4, page.size)
        Assertions.assertTrue(query.hasNextPage())
        Assertions.assertTrue(query.hasPreviousPage())
        query.lastPage()
        page = query.list()
        Assertions.assertEquals(2, page.size)
        Assertions.assertFalse(query.hasNextPage())
        Assertions.assertTrue(query.hasPreviousPage())
        query.firstPage()
        page = query.list()
        Assertions.assertEquals(4, page.size)
        Assertions.assertTrue(query.hasNextPage())
        Assertions.assertFalse(query.hasPreviousPage())
        query.page(Page.of(1, 5))
        Assertions.assertEquals(2, query.pageCount())
        page = query.list()
        Assertions.assertEquals(5, page.size)
        Assertions.assertFalse(query.hasNextPage())
        Assertions.assertTrue(query.hasPreviousPage())

        // mix page with range
        page = query.page(0, 3).range(0, 1).list()
        Assertions.assertEquals(2, page.size)
    }

    private fun testImperativeRange(query: PanacheQuery<TestImperativeEntity>) {
        query.range(0, 3)
        var range: List<TestImperativeEntity> = query.list()
        Assertions.assertEquals(4, range.size)
        range = query.range(4, 7).list()
        Assertions.assertEquals(4, range.size)
        range = query.range(8, 12).list()
        Assertions.assertEquals(2, range.size)
        range = query.range(10, 12).list()
        Assertions.assertEquals(0, range.size)

        // when using range, we cannot use any of the page related operations
        Assertions.assertThrows(UnsupportedOperationException::class.java) { query.range(0, 2).nextPage() }
        Assertions.assertThrows(UnsupportedOperationException::class.java) { query.range(0, 2).previousPage() }
        Assertions.assertThrows(UnsupportedOperationException::class.java) { query.range(0, 2).pageCount() }
        Assertions.assertThrows(UnsupportedOperationException::class.java) { query.range(0, 2).lastPage() }
        Assertions.assertThrows(UnsupportedOperationException::class.java) { query.range(0, 2).firstPage() }
        Assertions.assertThrows(UnsupportedOperationException::class.java) { query.range(0, 2).hasPreviousPage() }
        Assertions.assertThrows(UnsupportedOperationException::class.java) { query.range(0, 2).hasNextPage() }
        Assertions.assertThrows(UnsupportedOperationException::class.java) { query.range(0, 2).page() }

        // but this is valid to switch from range to page
        range = query.range(0, 2).page(0, 3).list()
        Assertions.assertEquals(3, range.size)
    }

    @GET
    @Path("reactive/entity")
    fun testReactiveEntity(): Response {
        val entities: List<TestReactiveEntity> = testReactiveEntities

        // insert all
        Assertions.assertEquals(0, TestReactiveEntity.count().await().indefinitely())
        TestReactiveEntity.persist(entities).await().indefinitely()
        Assertions.assertEquals(10, TestReactiveEntity.count().await().indefinitely())

        // varargs
        val entity11 = TestReactiveEntity("title11", "category", "desc")
        val entity12 = TestReactiveEntity("title11", "category", "desc")
        TestReactiveEntity.persist(entity11, entity12).await().indefinitely()
        Assertions.assertEquals(12, TestReactiveEntity.count().await().indefinitely())
        entity11.category = "categoryUpdated"
        entity12.category = "categoryUpdated"
        TestReactiveEntity.update(entity11, entity12).await().indefinitely()
        entity11.delete().await().indefinitely()
        entity12.delete().await().indefinitely()
        TestReactiveEntity.persistOrUpdate(entity11, entity12).await().indefinitely()
        Assertions.assertEquals(12, TestReactiveEntity.count().await().indefinitely())
        entity11.category = "categoryUpdated"
        entity12.category = "categoryUpdated"
        TestReactiveEntity.persistOrUpdate(entity11, entity12).await().indefinitely()
        entity11.delete().await().indefinitely()
        entity12.delete().await().indefinitely()
        Assertions.assertEquals(10, TestReactiveEntity.count().await().indefinitely())

        // paginate
        testReactivePagination(TestReactiveEntity.findAll())

        // range
        testReactiveRange(TestReactiveEntity.findAll())

        // query
        Assertions.assertEquals(5,
                TestReactiveEntity.list("category", "category0").await().indefinitely().size)
        Assertions.assertEquals(5,
                TestReactiveEntity.list("category = ?1", "category0").await().indefinitely().size)
        Assertions.assertEquals(5, TestReactiveEntity.list("category = :category",
                Parameters.with("category", "category1")).await().indefinitely().size)
        Assertions.assertEquals(5,
                TestReactiveEntity.list("{'category' : ?1}", "category0").await().indefinitely().size)
        Assertions.assertEquals(5, TestReactiveEntity.list("{'category' : :category}",
                Parameters.with("category", "category1")).await().indefinitely().size)
        val listQuery: Document = Document().append("category", "category1")
        Assertions.assertEquals(5, TestReactiveEntity.list(listQuery).await().indefinitely().size)
        Assertions.assertEquals(0, TestReactiveEntity.list("category", null).await().indefinitely().size)
        Assertions.assertEquals(0, TestReactiveEntity.list("category = :category",
                Parameters.with("category", null)).await().indefinitely().size)

        // regex
        val entityWithUpperCase = TestReactiveEntity("title11", "upperCaseCategory", "desc")
        entityWithUpperCase.persist<TestReactiveEntity>().await().indefinitely()
        Assertions.assertEquals(1, TestReactiveEntity.list("category like ?1", "upperCase.*")
                .await().indefinitely().size)
        Assertions.assertEquals(1, TestReactiveEntity.list("category like ?1", "/uppercase.*/i")
                .await().indefinitely().size)
        entityWithUpperCase.delete().await().indefinitely()

        // sort
        val entityA = TestReactiveEntity("aaa", "aaa", "aaa")
        entityA.persist<TestReactiveEntity>().await().indefinitely()
        val entityZ = TestReactiveEntity("zzz", "zzz", "zzz")
        entityZ.persistOrUpdate<TestReactiveEntity>().await().indefinitely()
        var result: TestReactiveEntity = TestReactiveEntity.listAll(Sort.ascending("title")).await()
                .indefinitely()[0]
        Assertions.assertEquals("aaa", result.title)
        result = TestReactiveEntity.listAll(Sort.descending("title")).await().indefinitely()[0]
        Assertions.assertEquals("zzz", result.title)
        entityA.delete().await().indefinitely()
        entityZ.delete().await().indefinitely()

        // collation
        val entityALower = TestReactiveEntity("aaa", "aaa", "aaa")
        entityALower.persist<TestReactiveEntity>().await().indefinitely()
        val entityAUpper = TestReactiveEntity("AAA", "AAA", "AAA")
        entityAUpper.persist<TestReactiveEntity>().await().indefinitely()
        val entityB = TestReactiveEntity("BBB", "BBB", "BBB")
        entityB.persistOrUpdate<TestReactiveEntity>().await().indefinitely()
        var results: List<TestReactiveEntity> = TestReactiveEntity.listAll(Sort.ascending("title")).await()
                .indefinitely()
        Assertions.assertEquals("AAA", results[0].title)
        Assertions.assertEquals("BBB", results[1].title)
        Assertions.assertEquals("aaa", results[2].title)
        val collation: Collation = Collation.builder().caseLevel(true).collationStrength(CollationStrength.PRIMARY).locale("fr")
                .build()
        results = TestReactiveEntity.findAll( Sort.ascending("title")).withCollation(collation).list()
                .await().indefinitely()
        Assertions.assertEquals("aaa", results[0].title)
        Assertions.assertEquals("AAA", results[1].title)
        Assertions.assertEquals("BBB", results[2].title)
        entityAUpper.delete().await().indefinitely()
        entityALower.delete().await().indefinitely()
        entityB.delete().await().indefinitely()

        // count
        Assertions.assertEquals(5, TestReactiveEntity.count("category", "category0").await().indefinitely())
        Assertions.assertEquals(5, TestReactiveEntity.count("category = ?1", "category0").await().indefinitely())
        Assertions.assertEquals(5, TestReactiveEntity.count("category = :category",
                Parameters.with("category", "category1")).await().indefinitely())
        Assertions.assertEquals(5, TestReactiveEntity.count("{'category' : ?1}", "category0").await().indefinitely())
        Assertions.assertEquals(5, TestReactiveEntity.count("{'category' : :category}",
                Parameters.with("category", "category1")).await().indefinitely())
        val countQuery: Document = Document().append("category", "category1")
        Assertions.assertEquals(5, TestReactiveEntity.count(countQuery).await().indefinitely())

        // update
        val list: List<TestReactiveEntity> = TestReactiveEntity.list("category = ?1", "category0").await()
        .indefinitely()
        Assertions.assertEquals(5, list.size)
        for (entity in list) {
            entity.category = "newCategory"
        }
        TestReactiveEntity.update(list).await().indefinitely()
        TestReactiveEntity.update(list.stream()).await().indefinitely()
        TestReactiveEntity.persistOrUpdate(list).await().indefinitely()
        var updated: Long = TestReactiveEntity.update("category", "newCategory2").where("category", "newCategory").await()
                .indefinitely()
        Assertions.assertEquals(5, updated)
        updated = TestReactiveEntity.update("category = ?1", "newCategory").where("category = ?1", "newCategory2").await()
                .indefinitely()
        Assertions.assertEquals(5, updated)
        updated = TestReactiveEntity.update("{'category' : ?1}", "newCategory2").where("{'category' : ?1}", "newCategory")
                .await().indefinitely()
        Assertions.assertEquals(5, updated)
        updated = TestReactiveEntity.update("category = :category", Parameters.with("category", "newCategory"))
                .where("category = :category", Parameters.with("category", "newCategory2")).await().indefinitely()
        Assertions.assertEquals(5, updated)
        updated = TestReactiveEntity.update("{'category' : :category}", Parameters.with("category", "newCategory2"))
                .where("{'category' : :category}", Parameters.with("category", "newCategory")).await().indefinitely()
        Assertions.assertEquals(5, updated)
        Assertions.assertEquals(5, TestReactiveEntity.count("category = ?1", "newCategory2").await().indefinitely())
        updated = TestReactiveEntity.update("newField", "newValue").all().await().indefinitely()
        Assertions.assertEquals(10, updated)

        // delete
        TestReactiveEntity.delete("category = ?1", "newCategory2").await().indefinitely()
        TestReactiveEntity.delete("{'category' : ?1}", "category1").await().indefinitely()
        Assertions.assertEquals(0, TestReactiveEntity.count().await().indefinitely())
        TestReactiveEntity.persist(entities.stream()).await().indefinitely()
        TestReactiveEntity.delete("category = :category", Parameters.with("category", "category0")).await().indefinitely()
        TestReactiveEntity.delete("{'category' : :category}", Parameters.with("category", "category1")).await().indefinitely()
        Assertions.assertEquals(0, TestReactiveEntity.count().await().indefinitely())
        TestReactiveEntity.persistOrUpdate(entities.stream()).await().indefinitely()
        TestReactiveEntity.delete("category", "category0").await().indefinitely()
        TestReactiveEntity.delete("category", "category1").await().indefinitely()
        Assertions.assertEquals(0, TestReactiveEntity.count().await().indefinitely())
        return Response.ok().build()
    }

    @GET
    @Path("reactive/repository")
    fun testReactiveRepository(): Response {
        val entities: List<TestReactiveEntity> = testReactiveEntities

        // insert all
        Assertions.assertEquals(0, testReactiveRepository.count().await().indefinitely())
        testReactiveRepository.persist(entities).await().indefinitely()
        Assertions.assertEquals(10, testReactiveRepository.count().await().indefinitely())

        // varargs
        val entity11 = TestReactiveEntity("title11", "category", "desc")
        val entity12 = TestReactiveEntity("title11", "category", "desc")
        testReactiveRepository.persist(entity11, entity12).await().indefinitely()
        Assertions.assertEquals(12, testReactiveRepository.count().await().indefinitely())
        entity11.category = "categoryUpdated"
        entity12.category = "categoryUpdated"
        testReactiveRepository.update(entity11, entity12).await().indefinitely()
        entity11.delete().await().indefinitely()
        entity12.delete().await().indefinitely()
        testReactiveRepository.persistOrUpdate(entity11, entity12).await().indefinitely()
        Assertions.assertEquals(12, testReactiveRepository.count().await().indefinitely())
        entity11.category = "categoryUpdated"
        entity12.category = "categoryUpdated"
        testReactiveRepository.persistOrUpdate(entity11, entity12).await().indefinitely()
        entity11.delete().await().indefinitely()
        entity12.delete().await().indefinitely()
        Assertions.assertEquals(10, testReactiveRepository.count().await().indefinitely())

        // paginate
        testReactivePagination(testReactiveRepository.findAll())

        // range
        testReactiveRange(testReactiveRepository.findAll())

        // query
        Assertions.assertEquals(5,
                testReactiveRepository.list("category", "category0").await().indefinitely().size)
        Assertions.assertEquals(5,
                testReactiveRepository.list("category = ?1", "category0").await().indefinitely().size)
        Assertions.assertEquals(5, testReactiveRepository.list("category = :category",
                Parameters.with("category", "category1")).await().indefinitely().size)
        Assertions.assertEquals(5,
                testReactiveRepository.list("{'category' : ?1}", "category0").await().indefinitely().size)
        Assertions.assertEquals(5, testReactiveRepository.list("{'category' : :category}",
                Parameters.with("category", "category1")).await().indefinitely().size)
        val listQuery: Document = Document().append("category", "category1")
        Assertions.assertEquals(5, testReactiveRepository.list(listQuery).await().indefinitely().size)
        Assertions.assertEquals(0, testReactiveRepository.list("category", null).await().indefinitely().size)
        Assertions.assertEquals(0, testReactiveRepository.list("category = :category",
                Parameters.with("category", null)).await().indefinitely().size)

        // regex
        val entityWithUpperCase = TestReactiveEntity("title11", "upperCaseCategory", "desc")
        testReactiveRepository.persist(entityWithUpperCase).await().indefinitely()
        Assertions.assertEquals(1, testReactiveRepository.list("category like ?1", "upperCase.*")
                .await().indefinitely().size)
        Assertions.assertEquals(1, testReactiveRepository.list("category like ?1", "/uppercase.*/i")
                .await().indefinitely().size)
        testReactiveRepository.delete(entityWithUpperCase).await().indefinitely()

        // sort
        val entityA = TestReactiveEntity("aaa", "aaa", "aaa")
        testReactiveRepository.persist(entityA).await().indefinitely()
        val entityZ = TestReactiveEntity("zzz", "zzz", "zzz")
        testReactiveRepository.persistOrUpdate(entityZ).await().indefinitely()
        var result: TestReactiveEntity = testReactiveRepository.listAll(Sort.ascending("title")).await().indefinitely()[0]
        Assertions.assertEquals("aaa", result.title)
        result = testReactiveRepository.listAll(Sort.descending("title")).await().indefinitely()[0]
        Assertions.assertEquals("zzz", result.title)
        testReactiveRepository.delete(entityA).await().indefinitely()
        testReactiveRepository.delete(entityZ).await().indefinitely()

        // collation
        val entityALower = TestReactiveEntity("aaa", "aaa", "aaa")
        testReactiveRepository.persist(entityALower).await().indefinitely()
        val entityAUpper = TestReactiveEntity("AAA", "AAA", "AAA")
        testReactiveRepository.persist(entityAUpper).await().indefinitely()
        val entityB = TestReactiveEntity("BBB", "BBB", "BBB")
        testReactiveRepository.persistOrUpdate(entityB).await().indefinitely()
        var results: List<TestReactiveEntity> = testReactiveRepository.listAll(Sort.ascending("title")).await().indefinitely()
        Assertions.assertEquals("AAA", results[0].title)
        Assertions.assertEquals("BBB", results[1].title)
        Assertions.assertEquals("aaa", results[2].title)
        val collation: Collation = Collation.builder().caseLevel(true).collationStrength(CollationStrength.PRIMARY).locale("fr")
                .build()
        results = testReactiveRepository.findAll(Sort.ascending("title")).withCollation(collation).list().await()
                .indefinitely()
        Assertions.assertEquals("aaa", results[0].title)
        Assertions.assertEquals("AAA", results[1].title)
        Assertions.assertEquals("BBB", results[2].title)
        testReactiveRepository.delete(entityALower).await().indefinitely()
        testReactiveRepository.delete(entityAUpper).await().indefinitely()
        testReactiveRepository.delete(entityB).await().indefinitely()

        // count
        Assertions.assertEquals(5,
                testReactiveRepository.count("category", "category0").await().indefinitely())
        Assertions.assertEquals(5,
                testReactiveRepository.count("category = ?1", "category0").await().indefinitely())
        Assertions.assertEquals(5, testReactiveRepository.count("category = :category",
                Parameters.with("category", "category1")).await().indefinitely())
        Assertions.assertEquals(5,
                testReactiveRepository.count("{'category' : ?1}", "category0").await().indefinitely())
        Assertions.assertEquals(5, testReactiveRepository.count("{'category' : :category}",
                Parameters.with("category", "category1")).await().indefinitely())
        val countQuery: Document = Document().append("category", "category1")
        Assertions.assertEquals(5, testReactiveRepository.count(countQuery).await().indefinitely())

        // update
        val list: List<TestReactiveEntity> = testReactiveRepository.list("category = ?1", "category0").await().indefinitely()
        Assertions.assertEquals(5, list.size)
        for (entity in list) {
            entity.category = "newCategory"
        }
        testReactiveRepository.update(list).await().indefinitely()
        testReactiveRepository.update(list.stream()).await().indefinitely()
        testReactiveRepository.persistOrUpdate(list).await().indefinitely()
        var updated: Long = testReactiveRepository.update("category", "newCategory2").where("category", "newCategory").await()
                .indefinitely()
        Assertions.assertEquals(5, updated)
        updated = testReactiveRepository.update("category = ?1", "newCategory").where("category = ?1", "newCategory2").await()
                .indefinitely()
        Assertions.assertEquals(5, updated)
        updated = testReactiveRepository.update("{'category' : ?1}", "newCategory2").where("{'category' : ?1}", "newCategory")
                .await().indefinitely()
        Assertions.assertEquals(5, updated)
        updated = testReactiveRepository.update("category = :category", Parameters.with("category", "newCategory"))
                .where("category = :category", Parameters.with("category", "newCategory2")).await().indefinitely()
        Assertions.assertEquals(5, updated)
        updated = testReactiveRepository.update("{'category' : :category}", Parameters.with("category", "newCategory2"))
                .where("{'category' : :category}", Parameters.with("category", "newCategory")).await().indefinitely()
        Assertions.assertEquals(5, updated)
        Assertions.assertEquals(5, testReactiveRepository.count("category = ?1", "newCategory2").await().indefinitely())
        updated = testReactiveRepository.update("newField", "newValue").all().await().indefinitely()
        Assertions.assertEquals(10, updated)

        // delete
        testReactiveRepository.delete("category = ?1", "newCategory2").await().indefinitely()
        testReactiveRepository.delete("{'category' : ?1}", "category1").await().indefinitely()
        Assertions.assertEquals(0, testReactiveRepository.count().await().indefinitely())
        testReactiveRepository.persist(entities.stream()).await().indefinitely()
        testReactiveRepository.delete("category = :category", Parameters.with("category", "category0")).await().indefinitely()
        testReactiveRepository.delete("{'category' : :category}", Parameters.with("category", "category1")).await()
                .indefinitely()
        Assertions.assertEquals(0, testReactiveRepository.count().await().indefinitely())
        testReactiveRepository.persistOrUpdate(entities.stream()).await().indefinitely()
        testReactiveRepository.delete("category", "category0").await().indefinitely()
        testReactiveRepository.delete("category", "category1").await().indefinitely()
        Assertions.assertEquals(0, testReactiveRepository.count().await().indefinitely())
        return Response.ok().build()
    }

    private val testReactiveEntities: List<TestReactiveEntity>
        get() {
            return (0..9).map {
                TestReactiveEntity("title$it",
                        "category" + it % 2,
                        "description$it")
            }
        }

    private fun testReactivePagination(query: ReactivePanacheQuery<TestReactiveEntity>) {
        query.page(0, 4)
        Assertions.assertEquals(3, query.pageCount().await().indefinitely())
        var page: List<TestReactiveEntity> = query.list().await().indefinitely()
        Assertions.assertEquals(4, page.size)
        Assertions.assertTrue(query.hasNextPage().await().indefinitely())
        Assertions.assertFalse(query.hasPreviousPage())
        query.nextPage()
        page = query.list().await().indefinitely()
        Assertions.assertEquals(4, page.size)
        Assertions.assertTrue(query.hasNextPage().await().indefinitely())
        Assertions.assertTrue(query.hasPreviousPage())
        query.lastPage().await().indefinitely()
        page = query.list().await().indefinitely()
        Assertions.assertEquals(2, page.size)
        Assertions.assertFalse(query.hasNextPage().await().indefinitely())
        Assertions.assertTrue(query.hasPreviousPage())
        query.firstPage()
        page = query.list().await().indefinitely()
        Assertions.assertEquals(4, page.size)
        Assertions.assertTrue(query.hasNextPage().await().indefinitely())
        Assertions.assertFalse(query.hasPreviousPage())
        query.page(Page.of(1, 5))
        Assertions.assertEquals(2, query.pageCount().await().indefinitely())
        page = query.list().await().indefinitely()
        Assertions.assertEquals(5, page.size)
        Assertions.assertFalse(query.hasNextPage().await().indefinitely())
        Assertions.assertTrue(query.hasPreviousPage())

        // mix page with range
        page = query.page(0, 3).range(0, 1).list().await().indefinitely()
        Assertions.assertEquals(2, page.size)
    }

    private fun testReactiveRange(query: ReactivePanacheQuery<TestReactiveEntity>) {
        query.range(0, 3)
        var range: List<TestReactiveEntity> = query.list().await().indefinitely()
        Assertions.assertEquals(4, range.size)
        range = query.range(4, 7).list().await().indefinitely()
        Assertions.assertEquals(4, range.size)
        range = query.range(8, 12).list().await().indefinitely()
        Assertions.assertEquals(2, range.size)
        range = query.range(10, 12).list().await().indefinitely()
        Assertions.assertEquals(0, range.size)

        // when using range, we cannot use any of the page related operations
        Assertions.assertThrows(UnsupportedOperationException::class.java) { query.range(0, 2).nextPage() }
        Assertions.assertThrows(UnsupportedOperationException::class.java) { query.range(0, 2).previousPage() }
        Assertions.assertThrows(UnsupportedOperationException::class.java) { query.range(0, 2).pageCount() }
        Assertions.assertThrows(UnsupportedOperationException::class.java) { query.range(0, 2).lastPage() }
        Assertions.assertThrows(UnsupportedOperationException::class.java) { query.range(0, 2).firstPage() }
        Assertions.assertThrows(UnsupportedOperationException::class.java) { query.range(0, 2).hasPreviousPage() }
        Assertions.assertThrows(UnsupportedOperationException::class.java) { query.range(0, 2).hasNextPage() }
        Assertions.assertThrows(UnsupportedOperationException::class.java) { query.range(0, 2).page() }

        // but this is valid to switch from range to page
        range = query.range(0, 2).page(0, 3).list().await().indefinitely()
        Assertions.assertEquals(3, range.size)
    }
}