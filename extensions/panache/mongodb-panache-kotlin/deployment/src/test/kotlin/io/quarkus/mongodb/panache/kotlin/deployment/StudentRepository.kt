package io.quarkus.mongodb.panache.kotlin.deployment

import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import io.quarkus.mongodb.panache.common.PanacheUpdate
import io.quarkus.mongodb.panache.kotlin.PanacheMongoRepositoryBase
import io.quarkus.mongodb.panache.kotlin.PanacheQuery
import io.quarkus.mongodb.panache.kotlin.runtime.KotlinMongoOperations.Companion.INSTANCE
import io.quarkus.panache.common.Parameters
import io.quarkus.panache.common.Sort
import org.bson.Document
import java.util.stream.Stream

/**
 * This class is used by TestEnhancers to validate the bytecode generation.  Each method on PanacheMongoRepositoryBase
 * is manually implemented to give us a compiler generated metric against which to validate the quarkus generated
 * bytecode. TestEnhancers further validates that all @GenerateBridge annotated methods are represented by a 'target_'
 * method here.
 */
@Suppress("unused", "UNCHECKED_CAST")
class StudentRepository : PanacheMongoRepositoryBase<Student, Long> {
    fun target_count(): Long = INSTANCE.count(Student::class.java)

    fun target_count(query: String, vararg params: Any?): Long =
            INSTANCE.count(Student::class.java, query, *params)

    fun target_count(query: String, params: Map<String, Any?>): Long =
            INSTANCE.count(Student::class.java, query, params)

    fun target_count(query: String, params: Parameters): Long =
            INSTANCE.count(Student::class.java, query, params)

    fun target_count(query: Document): Long = INSTANCE.count(Student::class.java, query)

    fun target_deleteAll(): Long = INSTANCE.deleteAll(Student::class.java)

    fun target_deleteById(id: Long): Boolean = INSTANCE.deleteById(Student::class.java, id)

    fun target_delete(query: String, vararg params: Any?): Long =
            INSTANCE.delete(Student::class.java, query, *params)

    fun target_delete(query: String, params: Map<String, Any?>): Long =
            INSTANCE.delete(Student::class.java, query, params)

    fun target_delete(query: String, params: Parameters): Long =
            INSTANCE.delete(Student::class.java, query, params)

    fun target_delete(query: Document): Long = INSTANCE.delete(Student::class.java, query)

    fun target_findById(id: Long): Student? =
            INSTANCE.findById(Student::class.java, id) as Student?

    fun target_find(query: String, vararg params: Any?): PanacheQuery<Student> =
            INSTANCE.find(Student::class.java, query, *params) as PanacheQuery<Student>

    fun target_find(query: String, sort: Sort, vararg params: Any?): PanacheQuery<Student> =
            INSTANCE.find(Student::class.java, query, sort, *params) as PanacheQuery<Student>

    fun target_find(query: String, params: Map<String, Any?>): PanacheQuery<Student> =
            INSTANCE.find(Student::class.java, query, params) as PanacheQuery<Student>

    fun target_find(query: String, sort: Sort, params: Map<String, Any?>): PanacheQuery<Student> =
            INSTANCE.find(Student::class.java, query, sort, params) as PanacheQuery<Student>

    fun target_find(query: String, params: Parameters): PanacheQuery<Student> =
            INSTANCE.find(Student::class.java, query, params) as PanacheQuery<Student>

    fun target_find(query: String, sort: Sort, params: Parameters): PanacheQuery<Student> =
            INSTANCE.find(Student::class.java, query, sort, params) as PanacheQuery<Student>

    fun target_find(query: Document): PanacheQuery<Student> =
            INSTANCE.find(Student::class.java, query) as PanacheQuery<Student>

    fun target_find(query: Document, sort: Document): PanacheQuery<Student> =
            INSTANCE.find(Student::class.java, query, sort) as PanacheQuery<Student>

    fun target_findAll(): PanacheQuery<Student> =
            INSTANCE.findAll(Student::class.java) as PanacheQuery<Student>

    fun target_findAll(sort: Sort): PanacheQuery<Student> =
            INSTANCE.findAll(Student::class.java, sort) as PanacheQuery<Student>

    fun target_list(query: String, vararg params: Any?): List<Student> =
            INSTANCE.list(Student::class.java, query, *params) as List<Student>

    fun target_list(query: String, sort: Sort, vararg params: Any?): List<Student> =
            INSTANCE.list(Student::class.java, query, sort, *params) as List<Student>

    fun target_list(query: String, params: Map<String, Any?>): List<Student> =
            INSTANCE.list(Student::class.java, query, params) as List<Student>

    fun target_list(query: String, sort: Sort, params: Map<String, Any?>): List<Student> =
            INSTANCE.list(Student::class.java, query, sort, params) as List<Student>

    fun target_list(query: String, params: Parameters): List<Student> =
            INSTANCE.list(Student::class.java, query, params) as List<Student>

    fun target_list(query: String, sort: Sort, params: Parameters): List<Student> =
            INSTANCE.list(Student::class.java, query, sort, params) as List<Student>

    fun target_list(query: Document): List<Student> =
            INSTANCE.list(Student::class.java, query) as List<Student>

    fun target_list(query: Document, sort: Document): List<Student> =
            INSTANCE.list(Student::class.java, query, sort) as List<Student>

    fun target_listAll(): List<Student> = INSTANCE.listAll(Student::class.java) as List<Student>

    fun target_listAll(sort: Sort): List<Student> =
            INSTANCE.listAll(Student::class.java, sort) as List<Student>

    fun target_mongoCollection(): MongoCollection<Student> =
            INSTANCE.mongoCollection(Student::class.java) as MongoCollection<Student>

    fun target_mongoDatabase(): MongoDatabase = INSTANCE.mongoDatabase(Student::class.java)

    fun target_stream(query: String, vararg params: Any?): Stream<Student> =
            INSTANCE.stream(Student::class.java, query, *params) as Stream<Student>

    fun target_stream(query: String, sort: Sort, vararg params: Any?): Stream<Student> =
            INSTANCE.stream(Student::class.java, query, sort, *params) as Stream<Student>

    fun target_stream(query: String, params: Map<String, Any?>): Stream<Student> =
            INSTANCE.stream(Student::class.java, query, params) as Stream<Student>

    fun target_stream(query: String, sort: Sort, params: Map<String, Any?>): Stream<Student> =
            INSTANCE.stream(Student::class.java, query, sort, params) as Stream<Student>

    fun target_stream(query: String, params: Parameters): Stream<Student> =
            INSTANCE.stream(Student::class.java, query, params) as Stream<Student>

    fun target_stream(query: String, sort: Sort, params: Parameters): Stream<Student> =
            INSTANCE.stream(Student::class.java, query, sort, params) as Stream<Student>

    fun target_stream(query: Document): Stream<Student> =
            INSTANCE.stream(Student::class.java, query) as Stream<Student>

    fun target_stream(query: Document, sort: Document): Stream<Student> =
            INSTANCE.stream(Student::class.java, query, sort) as Stream<Student>

    fun target_streamAll(sort: Sort): Stream<Student> =
            INSTANCE.streamAll(Student::class.java, sort) as Stream<Student>

    fun target_streamAll(): Stream<Student> =
            INSTANCE.streamAll(Student::class.java) as Stream<Student>

    fun target_update(update: String, vararg params: Any?): PanacheUpdate =
            INSTANCE.update(Student::class.java, update, *params)

    fun target_update(update: String, params: Map<String, Any?>): PanacheUpdate =
            INSTANCE.update(Student::class.java, update, params)

    fun target_update(update: String, params: Parameters): PanacheUpdate =
            INSTANCE.update(Student::class.java, update, params)
}
class Student
