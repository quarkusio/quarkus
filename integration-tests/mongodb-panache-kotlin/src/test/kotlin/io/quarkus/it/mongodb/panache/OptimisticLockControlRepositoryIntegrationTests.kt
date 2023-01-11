package io.quarkus.it.mongodb.panache

import com.mongodb.MongoWriteException
import io.quarkus.it.mongodb.panache.model.BikeV
import io.quarkus.it.mongodb.panache.model.Car
import io.quarkus.it.mongodb.panache.model.CarV
import io.quarkus.mongodb.panache.common.exception.OptimisticLockException
import io.quarkus.mongodb.panache.kotlin.PanacheMongoRepositoryBase
import io.quarkus.test.junit.QuarkusTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import java.util.*
import java.util.function.Consumer
import java.util.stream.Collectors
import java.util.stream.IntStream
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

@QuarkusTest
class OptimisticLockControlRepositoryIntegrationTests {

    @Inject
    lateinit var carRepository: CarRepository

    @Inject
    lateinit var carVRepository: CarVRepository

    @Inject
    lateinit var bikeVRepository: BikeVRepository

    @BeforeEach
    fun beforeEach() {
        carRepository.deleteAll()
        carVRepository.deleteAll()
    }

    @Test
    fun persistOneWithoutVersion() {
        val carWithoutVersion = buildDefault()
        carRepository.persist(carWithoutVersion)
        val carDb: Car = carRepository.findById(carWithoutVersion.id)!!

        assertNotNull(carDb)
        assertEquals(carWithoutVersion, carDb)
    }

    @Test
    fun persistOneWithVersion() {
        val carWithVersion = buildDefaultV()
        carVRepository.persist(carWithVersion)
        val carDb: CarV = carVRepository.findById(carWithVersion.id)!!

        assertNotNull(carDb)
        assertEquals(0, carDb.version)
    }

    @Test
    fun updateOneWithoutVersion() {
        var carWithoutVersion = buildDefault()
        carRepository.persist(carWithoutVersion)
        var carDb: Car = carRepository.findById(carWithoutVersion.id)!!

        assertNotNull(carDb)
        assertEquals(carWithoutVersion, carDb)

        carWithoutVersion = Car(carWithoutVersion.id, 2022)
        carRepository.update(carWithoutVersion)
        carDb = carRepository.findById(carWithoutVersion.id)!!
        assertNotNull(carDb)
        assertEquals(carWithoutVersion, carDb)
    }

    @Test
    fun updateOneWithVersion() {
        var car = buildDefaultV()
        carVRepository.persist(car)
        var carDb: CarV = carVRepository.findById(car.id)!!
        assertEquals(0, carDb.version)

        car = CarV(carDb.id, 2022, carDb.version!!)
        carVRepository.update(car)
        carDb = carVRepository.findById(car.id)!!
        assertEquals(1, carDb.version)
    }

    @Test
    fun updateOneWithOutdatedVersion() {
        val car = buildDefaultV()
        carVRepository.persist(car)
        val carDb: CarV = carVRepository.findById(car.id)!!
        assertNotNull(carDb)
        assertEquals(car.id, carDb.id)
        assertEquals(car.version, carDb.version)
        assertEquals(car.modelYear, carDb.modelYear!!)
        val optimisticLockException = assertThrows(OptimisticLockException::class.java) {
            val car2 = CarV(carDb.id, 2021, -1L)
            carVRepository.update(car2)
        }
        assertNotNull(optimisticLockException.message)
    }

    @Test
    fun updateOneNonExistingWithVersion() {
        val car = buildDefaultV()
        carVRepository.persist(car)

        var carDb: CarV = carVRepository.findById(car.id)!!
        assertEquals(0, carDb.version)
        assertThrows(OptimisticLockException::class.java) {
            val carV = CarV(UUID.randomUUID().toString(), 2022, null)
            carVRepository.update(carV)
        }
        carDb = carVRepository.findById(car.id)!!
        assertEquals(0, carDb.version)
    }

    @Test
    fun persistOrUpdateOneWithoutVersion() {
        var carWithoutVersion = buildDefault()
        carRepository.persistOrUpdate(carWithoutVersion)

        var carDb: Car = carRepository.findById(carWithoutVersion.id)!!
        assertNotNull(carDb)
        assertEquals(carWithoutVersion, carDb)

        carWithoutVersion = Car(carWithoutVersion.id, 2022)
        carRepository.persistOrUpdate(carWithoutVersion)
        carDb = carRepository.findById(carWithoutVersion.id)!!
        assertNotNull(carDb)
        assertEquals(carWithoutVersion, carDb)
    }

    @Test
    fun persistOrUpdateOneWithVersion() {
        // add the car
        var car = buildDefaultV()
        carVRepository.persistOrUpdate(car)
        var carDb: CarV = carVRepository.findById(car.id)!!
        assertEquals(0, carDb.version)

        // must update the car
        car = CarV(carDb.id, 2022, carDb.version!!)
        carVRepository.persistOrUpdate(car)
        carDb = carVRepository.findById(car.id)!!
        assertEquals(1, carDb.version)
        assertEquals(2022, carDb.modelYear)
    }

    @Test
    fun persistOrUpdateOnetWithOutdatedVersion() {
        // persist the car
        val car = buildDefaultV()
        carVRepository.persistOrUpdate(car)
        val carDb: CarV = carVRepository.findById(car.id)!!
        assertNotNull(carDb)
        assertEquals(car.id, carDb.id)
        assertEquals(car.version, carDb.version)
        assertEquals(car.modelYear, carDb.modelYear!!)

        // fail trying to update without version
        val mongoWriteException: MongoWriteException = assertThrows(
            MongoWriteException::class.java,
            Executable {
                val car2 = CarV(carDb.id, 2021, null)
                carVRepository.persistOrUpdate(car2)
            }
        )
        assertTrue(mongoWriteException.message!!.contains("E11000 duplicate key error collection"))
    }

    @Test
    fun persistOrUpdateOneConcurrentReqs() {
        val mapStatus: MutableMap<Int?, Boolean> = HashMap()
        val id = UUID.randomUUID().toString()
        val carV = CarV(id, 2021, null)
        carVRepository.persist(carV)
        val carVListToUpdate = IntStream.range(0, 10)
            .mapToObj { i: Int -> CarV(id, i, 0L) }
            .collect(Collectors.toList())
        carVListToUpdate.parallelStream()
            .forEach { carV1: CarV ->
                try {
                    carVRepository.persistOrUpdate(carV1)
                    mapStatus[carV1.modelYear] = true
                } catch (ex: OptimisticLockException) {
                    mapStatus[carV1.modelYear] = false
                }
            }

        val carVList2: List<CarV> = carVRepository.findAll().list()
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
        carVRepository.persist(carV)
        val carVListToUpdate = IntStream.range(0, 10)
            .mapToObj { i: Int -> CarV(id, i, 0L) }
            .collect(Collectors.toList())
        carVListToUpdate.parallelStream()
            .forEach { carV1: CarV ->
                try {
                    carVRepository.update(carV1)
                    mapStatus[carV1.modelYear] = true
                } catch (ex: OptimisticLockException) {
                    mapStatus[carV1.modelYear] = false
                }
            }

        val carVList2: List<CarV> = carVRepository.findAll().list()
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
        carRepository.persist(carList)
        val carDbList: List<Car> = carRepository.findAll().list()
        assertEquals(10, carDbList.size)
    }

    @Test
    fun persistManyWithVersion() {
        val carList = IntStream.range(0, 10)
            .mapToObj { i: Int -> CarV(UUID.randomUUID().toString(), i, null) }
            .collect(Collectors.toList())
        carVRepository.persist(carList)
        val carDbList: List<CarV> = carVRepository.findAll().list()
        assertEquals(10, carDbList.size)
        carDbList.forEach(Consumer { carV: CarV -> assertEquals(0L, carV.version) })
    }

    @Test
    fun updateManyWithoutVersion() {
        val id = UUID.randomUUID().toString()
        carRepository.persist(Car(id, 2021))

        val carList = IntStream.range(0, 10)
            .mapToObj { i: Int -> Car(id, i) }
            .collect(Collectors.toList())
        carRepository.update(carList)
        val carDbList: List<Car> = carRepository.findAll().list()
        assertEquals(1, carDbList.size)
    }

    @Test
    fun updateManyWithVersion() {
        // add one
        val id = UUID.randomUUID().toString()
        carVRepository.persist(CarV(id, 2021, null))

        // update this one and check the values
        carVRepository.update(Arrays.asList(CarV(id, 0, 0L)))
        var carDbList: List<CarV> = carVRepository.findAll().list()
        assertEquals(1, carDbList.size)

        var carV = carDbList[0]
        assertEquals(0, carV.modelYear)
        assertEquals(1L, carV.version)

        // update again
        carVRepository.update(Arrays.asList(CarV(id, 2, 1L)))
        carDbList = carVRepository.findAll().list()
        assertEquals(1, carDbList.size)

        carV = carDbList[0]
        assertEquals(2, carV.modelYear)
        assertEquals(2L, carV.version)

        // try to update with wrong version
        assertThrows(OptimisticLockException::class.java) { carVRepository.update(listOf(CarV(id, 1, 1L))) }

        // update using a list containing more elements, must fail
        val carList = IntStream.range(0, 10)
            .mapToObj { i: Int -> CarV(id, i, 2L) }
            .collect(Collectors.toList())
        assertThrows(OptimisticLockException::class.java) { carVRepository.update(carList) }
    }

    @Test
    fun persistOrUpdateManyWithoutVersion() {
        val carList = IntStream.range(0, 10)
            .mapToObj { i: Int -> Car(UUID.randomUUID().toString(), i) }
            .collect(Collectors.toList())
        carRepository.persistOrUpdate(carList)
        val carDbList: List<Car> = carRepository.findAll().list()
        assertEquals(10, carDbList.size)
    }

    /**
     * Try to persist two documents with the same id.
     */
    @Test
    fun persistManylWithNullVersionAndSameId() {
        val id = UUID.randomUUID().toString()
        val carV = CarV(id, 2022, null)
        val carV2 = CarV(id, 2022, null)
        try {
            // try to insert two with same id
            carVRepository.persist(listOf(carV, carV2))
        } catch (ex: Exception) {
            assertEquals(0L, carV.version) // inserted
            assertNull(carV2.version) // failed
            try {
                carVRepository.persist(listOf(carV, carV2))
            } catch (ignored: Exception) {
            }
        } finally {
            assertEquals(0L, carV.version) // didn't change
            assertNull(carV2.version) // failed
        }
    }

    /**
     * Try to persist two documents with generatedId.
     */
    @Test
    fun persistManylWithNullVersionAndAutoGeneratedId() {
        val bikeV = BikeV(null, null)
        val bikeV2 = BikeV(null, null)
        var bikeV3: BikeV? = null // will use bikeV id
        try {
            bikeVRepository.persist(listOf(bikeV, bikeV2))
            assertEquals(0L, bikeV.version) // inserted
            assertEquals(0L, bikeV2.version) // inserted
            bikeV3 = BikeV(bikeV.id, 2023, null)
            bikeVRepository.persist(listOf(bikeV, bikeV2, bikeV3))
        } catch (ignored: Exception) {
        } finally {
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
        carVRepository.persistOrUpdate(CarV(id, 2022, null))
        val carV = CarV("carv", 2022, null)
        val carV2 = CarV(id, 2022, 0L)
        val carV3 = CarV(id, 2023, 5L)
        val carV4 = CarV("carv4", 2022, 0L)
        try {
            carVRepository.persistOrUpdate(listOf(carV, carV2, carV3, carV4))
        } catch (ex: Exception) {
            assertEquals(0, carV.version) // inserted
            assertEquals(1L, carV2.version) // updated
            assertEquals(5L, carV3.version) // failed
            assertEquals(0L, carV4.version) // failed
            try {
                carVRepository.persistOrUpdate(listOf(carV, carV2, carV3, carV4))
            } catch (ignored: Exception) {
            }
        } finally {
            assertEquals(1L, carV.version) // updated in catch
            assertEquals(2L, carV2.version) // updated in catch
            assertEquals(5L, carV3.version) // failed
            assertEquals(0L, carV4.version) // failed
        }
    }

    /**
     * Mix of inserts and updates, keeping the version untouched for failed.
     */
    @Test
    fun persistOrUpdateManyWithVersionAndAutoGeneratedId() {
        // persist one to have some to update
        val bikeVPersisted = BikeV(null, null)
        bikeVRepository.persistOrUpdate(bikeVPersisted)
        val bikeV = BikeV(null, null)
        val bikeV2 = BikeV(bikeVPersisted.id, 2022, 0L)
        val bikeV3 = BikeV(null, null)
        val bikeV4 = BikeV(bikeVPersisted.id, 2024, 10L)
        try {
            bikeVRepository.persistOrUpdate(listOf(bikeV, bikeV2, bikeV3, bikeV4))
        } catch (ex: Exception) {
            assertEquals(0, bikeVPersisted.version) // didn't change
            assertEquals(1L, bikeV2.version) // updated
            assertEquals(0L, bikeV3.version) // inserted
            assertEquals(10L, bikeV4.version) // failed
            try {
                bikeVRepository.persistOrUpdate(listOf(bikeV, bikeV2, bikeV3, bikeV4))
            } catch (ignored: Exception) {
            }
        } finally {
            assertEquals(0, bikeVPersisted.version) // didn't change
            assertEquals(2L, bikeV2.version) // updated
            assertEquals(1L, bikeV3.version) // inserted
            assertEquals(10L, bikeV4.version) // failed
        }
    }

    @Test
    fun deleteById() {
        val carV = buildDefaultV()
        carVRepository.persist(carV)
        carV.id.let { carVRepository.deleteById(it) }
        assertTrue(carVRepository.findAll().list().isEmpty())
    }

    private fun buildDefaultV(): CarV {
        return CarV(UUID.randomUUID().toString(), 2022, null)
    }

    private fun buildDefault(): Car {
        return Car(UUID.randomUUID().toString(), 2022)
    }

    @ApplicationScoped
    class CarRepository : PanacheMongoRepositoryBase<Car, String>

    @ApplicationScoped
    class CarVRepository : PanacheMongoRepositoryBase<CarV, String>

    @ApplicationScoped
    class BikeVRepository : PanacheMongoRepositoryBase<BikeV, String>
}
