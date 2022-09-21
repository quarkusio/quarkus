package io.quarkus.it.mongodb.panache.reactive

import com.mongodb.MongoWriteException
import io.quarkus.it.mongodb.panache.model.Car
import io.quarkus.it.mongodb.panache.model.CarV
import io.quarkus.mongodb.panache.common.exception.OptimisticLockException
import io.quarkus.mongodb.panache.kotlin.reactive.ReactivePanacheMongoRepositoryBase
import io.quarkus.test.junit.QuarkusTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import java.util.function.Consumer
import java.util.stream.Collectors
import java.util.stream.IntStream
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

@QuarkusTest
class ReactiveOptimisticLockControlRepositoryIntegrationTests {

    @Inject
    lateinit var carRepository: CarRepository

    @Inject
    lateinit var carVRepository: CarVRepository

    @BeforeEach
    fun beforeEach() {
        carRepository.deleteAll().await().indefinitely()
        carVRepository.deleteAll().await().indefinitely()
    }

    @Test
    fun persistOneWithoutVersion() {
        val carWithoutVersion = buildDefault()
        carRepository.persist(carWithoutVersion).await().indefinitely()
        val carDb: Car? = carRepository.findById(carWithoutVersion.id).await().indefinitely()

        assertNotNull(carDb)
        assertEquals(carWithoutVersion, carDb)
    }

    @Test
    fun persistOneWithVersion() {
        val carWithVersion = buildDefaultV()
        carVRepository.persist(carWithVersion).await().indefinitely()
        val carDb: CarV? = carVRepository.findById(carWithVersion.id).await().indefinitely()

        assertNotNull(carDb)
        assertEquals(0, carDb!!.version)
    }

    @Test
    fun updateOneWithoutVersion() {
        var carWithoutVersion = buildDefault()
        carRepository.persist(carWithoutVersion).await().indefinitely()
        var carDb: Car? = carRepository.findById(carWithoutVersion.id).await().indefinitely()

        assertNotNull(carDb)
        assertEquals(carWithoutVersion, carDb)

        carWithoutVersion = Car(carWithoutVersion.id, 2022)
        carRepository.update(carWithoutVersion).await().indefinitely()
        carDb = carRepository.findById(carWithoutVersion.id).await().indefinitely()
        assertNotNull(carDb)
        assertEquals(carWithoutVersion, carDb)
    }

    @Test
    fun updateOneWithVersion() {
        var car = buildDefaultV()
        carVRepository.persist(car).await().indefinitely()
        var carDb: CarV? = carVRepository.findById(car.id).await().indefinitely()
        assertEquals(0, carDb!!.version)

        car = CarV(carDb.id, 2022, carDb.version!!)
        carVRepository.update(car).await().indefinitely()
        carDb = carVRepository.findById(car.id).await().indefinitely()
        assertEquals(1, carDb!!.version)
    }

    @Test
    fun updateOneWithOutdatedVersion() {
        val car = buildDefaultV()
        carVRepository.persist(car).await().indefinitely()
        val carDb: CarV = carVRepository.findById(car.id).await().indefinitely()!!
        assertNotNull(carDb)
        assertEquals(car.id, carDb.id)
        assertEquals(car.version, carDb.version)
        assertEquals(car.modelYear, carDb.modelYear!!)
        val optimisticLockException = assertThrows(OptimisticLockException::class.java) {
            val car2 = CarV(carDb.id, 2021, -1L)
            carVRepository.update(car2).await().indefinitely()
        }
        assertNotNull(optimisticLockException.message)
    }

    @Test
    fun updateOneNonExistingWithVersion() {
        val car = buildDefaultV()
        carVRepository.persist(car).await().indefinitely()
        var carDb: CarV = carVRepository.findById(car.id).await().indefinitely()!!
        assertEquals(0, carDb.version)
        assertThrows(OptimisticLockException::class.java) {
            val carV = CarV(UUID.randomUUID().toString(), 2022, null)
            carVRepository.update(carV).await().indefinitely()
        }
        carDb = carVRepository.findById(car.id).await().indefinitely()!!
        assertEquals(0, carDb.version)
    }

    @Test
    fun persistOrUpdateOneWithoutVersion() {
        var carWithoutVersion = buildDefault()
        carRepository.persistOrUpdate(carWithoutVersion).await().indefinitely()
        var carDb: Car = carRepository.findById(carWithoutVersion.id).await().indefinitely()!!
        assertEquals(carWithoutVersion, carDb)

        carWithoutVersion = Car(carWithoutVersion.id, 2022)
        carRepository.persistOrUpdate(carWithoutVersion).await().indefinitely()
        carDb = carRepository.findById(carWithoutVersion.id).await().indefinitely()!!
        assertEquals(carWithoutVersion, carDb)
    }

    @Test
    fun persistOrUpdateOneWithVersion() {
        // add the car
        var car = buildDefaultV()
        carVRepository.persistOrUpdate(car).await().indefinitely()
        var carDb: CarV = carVRepository.findById(car.id).await().indefinitely()!!
        assertEquals(0, carDb.version)

        // must update the car
        car = CarV(carDb.id, 2022, carDb.version!!)
        carVRepository.persistOrUpdate(car).await().indefinitely()
        carDb = carVRepository.findById(car.id).await().indefinitely()!!
        assertEquals(1, carDb.version)
        assertEquals(2022, carDb.modelYear)
    }

    @Test
    fun persistOrUpdateOnetWithOutdatedVersion() {
        // persist the car
        val car = buildDefaultV()
        carVRepository.persistOrUpdate(car).await().indefinitely()
        val carDb: CarV = carVRepository.findById(car.id).await().indefinitely()!!
        assertEquals(car.id, carDb.id)
        assertEquals(car.version, carDb.version)
        assertEquals(car.modelYear, carDb.modelYear!!)

        // fail trying to update without version
        val mongoWriteException: MongoWriteException = assertThrows(
            MongoWriteException::class.java
        ) {
            val car2 = CarV(carDb.id, 2021, null)
            carVRepository.persistOrUpdate(car2).await().indefinitely()
        }
        assertTrue(mongoWriteException.message!!.contains("E11000 duplicate key error collection"))
    }

    @Test
    fun persistOrUpdateOneConcurrentReqs() {
        val mapStatus: MutableMap<Int?, Boolean> = HashMap()
        val id = UUID.randomUUID().toString()
        val carV = CarV(id, 2021, null)
        carVRepository.persist(carV).await().indefinitely()
        val carVListToUpdate = IntStream.range(0, 10)
            .mapToObj { i: Int -> CarV(id, i, 0L) }
            .collect(Collectors.toList())

        carVListToUpdate.parallelStream()
            .forEach { carV1: CarV ->
                try {
                    carVRepository.persistOrUpdate(carV1).await().indefinitely()
                    mapStatus[carV1.modelYear] = true
                } catch (ex: OptimisticLockException) {
                    mapStatus[carV1.modelYear] = false
                }
            }
        val carVList2: List<CarV> = carVRepository.findAll().list().await().indefinitely()
        assertEquals(1, carVList2.size)

        val updateWithSuccessList = mapStatus.entries.stream()
            .filter { (_, value): Map.Entry<Int?, Boolean> -> value }
            .map { (key): Map.Entry<Int?, Boolean> -> key }
            .collect(Collectors.toList())
        assertEquals(1, updateWithSuccessList.size)
        assertEquals(updateWithSuccessList[0], carVList2[0].modelYear)
    }

    @Test
    fun updateOneConcurrentReqs() {
        val mapStatus: MutableMap<Int?, Boolean> = HashMap()
        val id = UUID.randomUUID().toString()
        val carV = CarV(id, 2021, null)
        carVRepository.persist(carV).await().indefinitely()
        val carVListToUpdate = IntStream.range(0, 10)
            .mapToObj { i: Int -> CarV(id, i, 0L) }
            .collect(Collectors.toList())

        carVListToUpdate.parallelStream()
            .forEach { carV1: CarV ->
                try {
                    carVRepository.update(carV1).await().indefinitely()
                    mapStatus[carV1.modelYear] = true
                } catch (ex: OptimisticLockException) {
                    mapStatus[carV1.modelYear] = false
                }
            }
        val carVList2: List<CarV> = carVRepository.findAll().list().await().indefinitely()
        assertEquals(1, carVList2.size)

        val updateWithSuccessList = mapStatus.entries.stream()
            .filter { (_, value): Map.Entry<Int?, Boolean> -> value }
            .map { (key): Map.Entry<Int?, Boolean> -> key }
            .collect(Collectors.toList())
        assertEquals(1, updateWithSuccessList.size)
        assertEquals(updateWithSuccessList[0], carVList2[0].modelYear)
    }

    @Test
    fun persistManyWithoutVersion() {
        val carList = IntStream.range(0, 10)
            .mapToObj { i: Int -> Car(UUID.randomUUID().toString(), i) }
            .collect(Collectors.toList())
        carRepository.persist(carList).await().indefinitely()
        val carDbList: List<Car> = carRepository.findAll().list().await().indefinitely()
        assertEquals(10, carDbList.size)
    }

    @Test
    fun persistManyWithVersion() {
        val carList = IntStream.range(0, 10)
            .mapToObj { i: Int -> CarV(UUID.randomUUID().toString(), i, null) }
            .collect(Collectors.toList())
        carVRepository.persist(carList).await().indefinitely()
        val carDbList: List<CarV> = carVRepository.findAll().list().await().indefinitely()
        assertEquals(10, carDbList.size)
        carDbList.forEach(Consumer { carV: CarV -> assertEquals(0L, carV.version) })
    }

    @Test
    fun updateManyWithoutVersion() {
        val id = UUID.randomUUID().toString()
        carRepository.persist(Car(id, 2021)).await().indefinitely()
        val carList = IntStream.range(0, 10)
            .mapToObj { i: Int -> Car(id, i) }
            .collect(Collectors.toList())
        carRepository.update(carList).await().indefinitely()
        val carDbList: List<Car> = carRepository.findAll().list().await().indefinitely()
        assertEquals(1, carDbList.size)
    }

    @Test
    fun updateManyWithVersion() {
        // add one
        val id = UUID.randomUUID().toString()
        carVRepository.persist(CarV(id, 2021, null)).await().indefinitely()

        // update this one and check the values
        carVRepository.update(Arrays.asList(CarV(id, 0, 0L))).await().indefinitely()
        var carDbList: List<CarV> = carVRepository.findAll().list().await().indefinitely()
        assertEquals(1, carDbList.size)

        var carV = carDbList[0]
        assertEquals(0, carV.modelYear)
        assertEquals(1L, carV.version)

        // update again
        carVRepository.update(Arrays.asList(CarV(id, 2, 1L))).await().indefinitely()
        carDbList = carVRepository.findAll().list().await().indefinitely()
        assertEquals(1, carDbList.size)

        carV = carDbList[0]
        assertEquals(2, carV.modelYear)
        assertEquals(2L, carV.version)

        // try to update with wrong version
        assertThrows(
            OptimisticLockException::class.java
        ) { carVRepository.update(listOf(CarV(id, 1, 1L))).await().indefinitely() }

        // update using a list containing more elements, must fail
        val carList = IntStream.range(0, 10)
            .mapToObj { i: Int -> CarV(id, i, 2L) }
            .collect(Collectors.toList())
        assertThrows(OptimisticLockException::class.java) { carVRepository.update(carList).await().indefinitely() }
    }

    @Test
    fun persistOrUpdateManyWithoutVersion() {
        val carList = IntStream.range(0, 10)
            .mapToObj { i: Int -> Car(UUID.randomUUID().toString(), i) }
            .collect(Collectors.toList())
        carRepository.persistOrUpdate(carList).await().indefinitely()
        val carDbList: List<Car> = carRepository.findAll().list().await().indefinitely()
        assertEquals(10, carDbList.size)
    }

    @Test
    fun persistOrUpdateManyWithVersion() {
        val id = UUID.randomUUID().toString()
        carVRepository.persist(CarV(id, 2021, null)).await().indefinitely()
        val carList = IntStream.range(0, 1)
            .mapToObj { i: Int -> CarV(id, i, 0L) }
            .collect(Collectors.toList())
        carVRepository.persistOrUpdate(carList).await().indefinitely()
        val carDbList: List<CarV> = carVRepository.findAll().list().await().indefinitely()
        assertEquals(1, carDbList.size)
        assertEquals(0, carDbList[0].modelYear)
        assertEquals(1L, carDbList[0].version)
    }

    @Test
    fun deleteWithoutVersionNonExistentDocument() {
        val car = buildDefault()
        carRepository.delete(car).await().indefinitely()
    }

    @Test
    fun deleteWithVersionNonExistentDocument() {
        val car = buildDefaultV()
        assertThrows(OptimisticLockException::class.java) { carVRepository.delete(car).await().indefinitely() }
    }

    @Test
    fun deleteById() {
        val carV = buildDefaultV()
        carVRepository.persist(carV).await().indefinitely()
        carV.id.let { carVRepository.deleteById(it).await().indefinitely() }
        assertTrue(carVRepository.findAll().list().await().indefinitely().isEmpty())
    }

    private fun buildDefaultV(): CarV {
        return CarV(UUID.randomUUID().toString(), 2022, null)
    }

    private fun buildDefault(): Car {
        return Car(UUID.randomUUID().toString(), 2022)
    }

    @ApplicationScoped
    class CarRepository : ReactivePanacheMongoRepositoryBase<Car, String>

    @ApplicationScoped
    class CarVRepository : ReactivePanacheMongoRepositoryBase<CarV, String>
}
