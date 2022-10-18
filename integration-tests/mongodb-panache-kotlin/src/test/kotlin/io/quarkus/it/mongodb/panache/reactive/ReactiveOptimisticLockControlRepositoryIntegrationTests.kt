package io.quarkus.it.mongodb.panache.reactive

import com.mongodb.MongoWriteException
import io.quarkus.it.mongodb.panache.model.BikeV
import io.quarkus.it.mongodb.panache.model.Car
import io.quarkus.it.mongodb.panache.model.CarV
import io.quarkus.mongodb.panache.common.exception.OptimisticLockException
import io.quarkus.mongodb.panache.kotlin.reactive.ReactivePanacheMongoRepositoryBase
import io.quarkus.test.junit.QuarkusTest
import io.smallrye.mutiny.CompositeException
import org.junit.jupiter.api.Assertions.*
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

    @Inject
    lateinit var bikeVRepository: BikeVRepository

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
        carVRepository.update(listOf(CarV(id, 2, 1L))).await().indefinitely()
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
        val compositeException = assertThrows(CompositeException::class.java) { carVRepository.update(carList).await().indefinitely() }
        compositeException.causes.stream()
            .allMatch { throwable: Throwable ->
                OptimisticLockException::class.java.isAssignableFrom(throwable.javaClass)
            }
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

    /**
     * Try to persist two documents with the same id.
     */
    @Test
    fun persistManyWithNullVersionAndSameId() {
        val id = UUID.randomUUID().toString()
        val carV = CarV(id, 2022, null)
        val carV2 = CarV(id, 2023, null)
        try {
            // try to insert two with same id
            carVRepository.persist(carV, carV2).await().indefinitely()
        } catch (ex: Exception) {
            assertPersistManyWithNullVersionAndSameId(carV, carV2)

            try {
                carVRepository.persist(carV, carV2).await().indefinitely()
            } catch (ignored: Exception) {
                assertPersistManyWithNullVersionAndSameId(carV, carV2)
            }
        }
    }

    private fun assertPersistManyWithNullVersionAndSameId(carV: CarV, carV2: CarV) {
        if (carV.version != null) {
            assertEquals(0L, carV.version) // inserted
            assertNull(carV2.version) // failed
        } else {
            assertEquals(0L, carV2.version) // inserted
            assertNull(carV.version) // failed
        }
    }

    /**
     * Try to persist two documents with generatedId.
     */
    @Test
    fun persistManyWithNullVersionAndAutoGeneratedId() {
        val bikeV = BikeV(null, null)
        val bikeV2 = BikeV(null, null)
        var bikeV3: BikeV? = null // will use bikeV id
        try {
            bikeVRepository.persist(java.util.List.of(bikeV, bikeV2)).await().indefinitely()
            assertEquals(0L, bikeV.version) // inserted
            assertEquals(0L, bikeV2.version) // inserted
            bikeV3 = BikeV(bikeV.id, 2023, null)
            bikeVRepository.persist(listOf(bikeV, bikeV2, bikeV3)).await().indefinitely()
        } catch (ignored: Exception) {
            assertEquals(0L, bikeV.version) // didn't change
            assertEquals(0L, bikeV2.version) // didn't change
            assertNull(bikeV3!!.version) // failed
        }
    }

    /**
     * Mix of inserts and updates, keeping the version untouched for failed.
     */
    @Test
    fun persistOrUpdateManyWithVersion() {
        val id = UUID.randomUUID().toString()

        // persist one to have some to update
        carVRepository.persistOrUpdate(CarV(id, 2022, null)).await().indefinitely()
        val carV = CarV("carv", 2022, null)
        val carV2 = CarV(id, 2022, 0L)
        val carV3 = CarV(id, 2023, 5L)
        val carV4 = CarV("carv4", 2022, 0L)
        try {
            carVRepository.persistOrUpdate(listOf(carV, carV2, carV3, carV4)).await().indefinitely()
        } catch (ex: Exception) {
            assertEquals(0, carV.version) // inserted
            assertEquals(0L, carV4.version) // failed

            //since there is no order guaranteed because the combine() we check which one was updated
            if (1L == carV2.version) {
                assertEquals(1L, carV2.version) //updated
                assertEquals(5L, carV3.version) //failed
            } else {
                assertEquals(null, carV2.version) //failed
                assertEquals(6L, carV3.version) //updated
            }

            try {
                carVRepository.persistOrUpdate(listOf(carV, carV2, carV3, carV4)).await().indefinitely()
            } catch (ignored: Exception) {
            }
        } finally {
            assertEquals(1L, carV.version) // updated in catch
            assertEquals(0L, carV4.version) // failed
            if (2L == carV2.version) {
                assertEquals(2L, carV2.version) //updated
                assertEquals(5L, carV3.version) //failed
            } else {
                assertEquals(null, carV2.version) //failed
                assertEquals(7L, carV3.version) //updated
            }
        }
    }

    /**
     * Mix of inserts and updates, keeping the version untouched for failed.
     */
    @Test
    fun persistOrUpdateManyWithVersionAndAutoGeneratedId() {
        // persist one to have some to update
        val bikeVPersisted = BikeV(null, null)
        bikeVRepository.persistOrUpdate(bikeVPersisted).await().indefinitely()
        val bikeV = BikeV(null, null)
        val bikeV2 = BikeV(bikeVPersisted.id, 2022, 0L)
        val bikeV3 = BikeV(null, null)
        val bikeV4 = BikeV(bikeVPersisted.id, 2024, 10L)
        try {
            bikeVRepository.persistOrUpdate(listOf(bikeV, bikeV2, bikeV3, bikeV4)).await().indefinitely()
        } catch (ex: Exception) {
            assertEquals(0, bikeVPersisted.version) // didn't change
            assertEquals(1L, bikeV2.version) // updated
            assertEquals(0L, bikeV3.version) // inserted
            assertEquals(10L, bikeV4.version) // failed
            try {
                bikeVRepository.persistOrUpdate(listOf(bikeV, bikeV2, bikeV3, bikeV4)).await().indefinitely()
            } catch (ignored: Exception) {
                assertEquals(0, bikeVPersisted.version) // didn't change
                assertEquals(2L, bikeV2.version) // updated
                assertEquals(1L, bikeV3.version) // inserted
                assertEquals(10L, bikeV4.version) // failed
            }
        }
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

    @ApplicationScoped
    class BikeVRepository : ReactivePanacheMongoRepositoryBase<BikeV, String>
}
