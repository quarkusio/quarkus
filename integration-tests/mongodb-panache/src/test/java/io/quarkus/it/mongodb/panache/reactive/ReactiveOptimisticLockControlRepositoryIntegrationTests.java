package io.quarkus.it.mongodb.panache.reactive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mongodb.MongoWriteException;

import io.quarkus.it.mongodb.panache.model.BikeV;
import io.quarkus.it.mongodb.panache.model.Car;
import io.quarkus.it.mongodb.panache.model.CarV;
import io.quarkus.mongodb.panache.common.exception.OptimisticLockException;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepositoryBase;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.CompositeException;

@QuarkusTest
public class ReactiveOptimisticLockControlRepositoryIntegrationTests {

    @Inject
    CarRepository carRepository;

    @Inject
    CarVRepository carVRepository;

    @Inject
    BikeVRepository bikeVRepository;

    @BeforeEach
    public void beforeEach() {
        carRepository.deleteAll().await().indefinitely();
        carVRepository.deleteAll().await().indefinitely();
        bikeVRepository.deleteAll().await().indefinitely();
    }

    @Test
    public void persistOneWithoutVersion() {
        Car carWithoutVersion = buildDefault();
        carRepository.persist(carWithoutVersion).await().indefinitely();

        Car carDb = carRepository.findById(carWithoutVersion.id).await().indefinitely();
        assertNotNull(carDb);
        assertEquals(carWithoutVersion, carDb);
    }

    @Test
    public void persistOneWithVersion() {
        CarV carWithVersion = buildDefaultV();
        carVRepository.persist(carWithVersion).await().indefinitely();

        CarV carDb = carVRepository.findById(carWithVersion.id).await().indefinitely();
        assertNotNull(carDb);
        assertEquals(0, carDb.version);
    }

    @Test
    public void updateOneWithoutVersion() {
        Car carWithoutVersion = buildDefault();
        carRepository.persist(carWithoutVersion).await().indefinitely();

        Car carDb = carRepository.findById(carWithoutVersion.id).await().indefinitely();
        assertNotNull(carDb);
        assertEquals(carWithoutVersion, carDb);

        carWithoutVersion = new Car(carWithoutVersion.id, 2022);
        carRepository.update(carWithoutVersion);

        carDb = carRepository.findById(carWithoutVersion.id).await().indefinitely();
        assertNotNull(carDb);
        assertEquals(carWithoutVersion, carDb);
    }

    @Test
    public void updateOneWithVersion() {
        CarV car = buildDefaultV();
        carVRepository.persist(car).await().indefinitely();

        CarV carDb = carVRepository.findById(car.id).await().indefinitely();
        assertEquals(0, carDb.version);

        car = new CarV(carDb.id, 2022, carDb.version);
        carVRepository.update(car).await().indefinitely();

        carDb = carVRepository.findById(car.id).await().indefinitely();
        assertEquals(1, carDb.version);
    }

    @Test
    public void updateOneWithOutdatedVersion() {
        CarV car = buildDefaultV();
        carVRepository.persist(car).await().indefinitely();

        CarV carDb = carVRepository.findById(car.id).await().indefinitely();
        assertNotNull(carDb);
        assertEquals(car, carDb);

        OptimisticLockException optimisticLockException = assertThrows(OptimisticLockException.class, () -> {
            CarV car2 = new CarV(carDb.id, 2021, -1l);
            carVRepository.update(car2).await().indefinitely();
        });

        assertNotNull(optimisticLockException.getMessage());
    }

    @Test
    public void updateOneNonExistingWithVersion() {
        CarV car = buildDefaultV();
        carVRepository.persist(car).await().indefinitely();

        CarV carDb = carVRepository.findById(car.id).await().indefinitely();
        assertEquals(0, carDb.version);

        assertThrows(OptimisticLockException.class, () -> {
            CarV carV = new CarV(UUID.randomUUID().toString(), 2022, null);
            carVRepository.update(carV).await().indefinitely();
        });

        carDb = carVRepository.findById(car.id).await().indefinitely();
        assertEquals(0, carDb.version);
    }

    @Test
    public void persistOrUpdateOneWithoutVersion() {
        Car carWithoutVersion = buildDefault();
        carRepository.persistOrUpdate(carWithoutVersion).await().indefinitely();

        Car carDb = carRepository.findById(carWithoutVersion.id).await().indefinitely();
        assertNotNull(carDb);
        assertEquals(carWithoutVersion, carDb);

        carWithoutVersion = new Car(carWithoutVersion.id, 2022);
        carRepository.persistOrUpdate(carWithoutVersion).await().indefinitely();

        carDb = carRepository.findById(carWithoutVersion.id).await().indefinitely();
        assertNotNull(carDb);
        assertEquals(carWithoutVersion, carDb);
    }

    @Test
    public void persistOrUpdateOneWithVersion() {
        //add the car
        CarV car = buildDefaultV();
        carVRepository.persistOrUpdate(car).await().indefinitely();
        CarV carDb = carVRepository.findById(car.id).await().indefinitely();
        assertEquals(0, carDb.version);

        //must update the car
        car = new CarV(carDb.id, 2022, carDb.version);
        carVRepository.persistOrUpdate(car).await().indefinitely();

        carDb = carVRepository.findById(car.id).await().indefinitely();
        assertEquals(1, carDb.version);
        assertEquals(2022, carDb.modelYear);
    }

    @Test
    public void persistOrUpdateOnetWithOutdatedVersion() {
        //persist the car
        CarV car = buildDefaultV();
        carVRepository.persistOrUpdate(car).await().indefinitely();
        CarV carDb = carVRepository.findById(car.id).await().indefinitely();
        assertNotNull(carDb);
        assertEquals(car, carDb);

        //fail trying to update without version
        MongoWriteException mongoWriteException = assertThrows(MongoWriteException.class, () -> {
            CarV car2 = new CarV(carDb.id, 2021, null);
            carVRepository.persistOrUpdate(car2).await().indefinitely();
        });

        assertTrue(mongoWriteException.getMessage().contains("E11000 duplicate key error collection"));
    }

    @Test
    public void persistOrUpdateOneConcurrentReqs() {
        Map<Integer, Boolean> mapStatus = new HashMap<>();
        String id = UUID.randomUUID().toString();
        CarV carV = new CarV(id, 2021, null);
        carVRepository.persist(carV).await().indefinitely();

        List<CarV> carVListToUpdate = IntStream.range(0, 10)
                .mapToObj(i -> new CarV(id, i, 0L))
                .collect(Collectors.toList());

        carVListToUpdate.parallelStream()
                .forEach(carV1 -> {
                    try {
                        carVRepository.persistOrUpdate(carV1).await().indefinitely();
                        mapStatus.put(carV1.modelYear, true);
                    } catch (OptimisticLockException ex) {
                        mapStatus.put(carV1.modelYear, false);
                    }
                });

        List<CarV> carVList2 = carVRepository.findAll().list().await().indefinitely();
        assertEquals(1, carVList2.size());

        List<Integer> updateWithSuccessList = mapStatus.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        assertEquals(1, updateWithSuccessList.size());
        assertEquals(updateWithSuccessList.get(0), carVList2.get(0).modelYear);
    }

    @Test
    public void updateOneConcurrentReqs() {
        Map<Integer, Boolean> mapStatus = new HashMap<>();
        String id = UUID.randomUUID().toString();
        CarV carV = new CarV(id, 2021, null);
        carVRepository.persist(carV).await().indefinitely();

        List<CarV> carVListToUpdate = IntStream.range(0, 10)
                .mapToObj(i -> new CarV(id, i, 0L))
                .collect(Collectors.toList());

        carVListToUpdate.parallelStream()
                .forEach(carV1 -> {
                    try {
                        carVRepository.update(carV1).await().indefinitely();
                        mapStatus.put(carV1.modelYear, true);
                    } catch (OptimisticLockException ex) {
                        mapStatus.put(carV1.modelYear, false);
                    }
                });

        List<CarV> carVList2 = carVRepository.findAll().list().await().indefinitely();
        assertEquals(1, carVList2.size());

        List<Integer> updateWithSuccessList = mapStatus.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        assertEquals(1, updateWithSuccessList.size());
        assertEquals(updateWithSuccessList.get(0), carVList2.get(0).modelYear);
    }

    @Test
    public void persistManyWithoutVersion() {
        List<Car> carList = IntStream.range(0, 10)
                .mapToObj(i -> new Car(UUID.randomUUID().toString(), i))
                .collect(Collectors.toList());
        carRepository.persist(carList).await().indefinitely();

        List<Car> carDbList = carRepository.findAll().list().await().indefinitely();
        assertEquals(10, carDbList.size());
    }

    @Test
    public void persistManyWithVersion() {
        List<CarV> carList = IntStream.range(0, 10)
                .mapToObj(i -> new CarV(UUID.randomUUID().toString(), i, null))
                .collect(Collectors.toList());
        carVRepository.persist(carList).await().indefinitely();

        List<CarV> carDbList = carVRepository.findAll().list().await().indefinitely();
        assertEquals(10, carDbList.size());
        carDbList.forEach(carV -> assertEquals(0L, carV.version));
    }

    @Test
    public void updateManyWithoutVersion() {
        String id = UUID.randomUUID().toString();
        carRepository.persist(new Car(id, 2021)).await().indefinitely();

        List<Car> carList = IntStream.range(0, 10)
                .mapToObj(i -> new Car(id, i))
                .collect(Collectors.toList());
        carRepository.update(carList).await().indefinitely();

        List<Car> carDbList = carRepository.findAll().list().await().indefinitely();
        assertEquals(1, carDbList.size());
    }

    @Test
    public void updateManyWithVersion() {
        //add one
        String id = UUID.randomUUID().toString();
        carVRepository.persist(new CarV(id, 2021, null)).await().indefinitely();

        //update this one and check the values
        carVRepository.update(Arrays.asList(new CarV(id, 0, 0L))).await().indefinitely();

        List<CarV> carDbList = carVRepository.findAll().list().await().indefinitely();
        assertEquals(1, carDbList.size());
        CarV carV = carDbList.get(0);
        assertEquals(0, carV.modelYear);
        assertEquals(1L, carV.version);

        //update again
        carVRepository.update(List.of(new CarV(id, 2, 1L))).await().indefinitely();
        carDbList = carVRepository.findAll().list().await().indefinitely();
        assertEquals(1, carDbList.size());
        carV = carDbList.get(0);
        assertEquals(2, carV.modelYear);
        assertEquals(2L, carV.version);

        //try to update with wrong version
        assertThrows(OptimisticLockException.class,
                () -> carVRepository.update(List.of(new CarV(id, 1, 1L))).await().indefinitely());

        //update using a list containing more elements, must fail
        List<CarV> carList = IntStream.range(0, 10)
                .mapToObj(i -> new CarV(id, i, 2L))
                .collect(Collectors.toList());
        CompositeException compositeException = assertThrows(CompositeException.class,
                () -> carVRepository.update(carList).await().indefinitely());
        compositeException.getCauses().stream()
                .allMatch(throwable -> OptimisticLockException.class.isAssignableFrom(throwable.getClass()));
    }

    @Test
    public void persistOrUpdateManyWithoutVersion() {
        List<Car> carList = IntStream.range(0, 10)
                .mapToObj(i -> new Car(UUID.randomUUID().toString(), i))
                .collect(Collectors.toList());
        carRepository.persistOrUpdate(carList).await().indefinitely();

        List<Car> carDbList = carRepository.findAll().list().await().indefinitely();
        assertEquals(10, carDbList.size());
    }

    /**
     * Try to persist two documents with the same id.
     */
    @Test
    public void persistManyWithNullVersionAndSameId() {
        String id = UUID.randomUUID().toString();
        CarV carV = new CarV(id, 2022, null);
        CarV carV2 = new CarV(id, 2023, null);

        try {
            //try to insert two with same id
            carVRepository.persist(List.of(carV, carV2)).await().indefinitely();
        } catch (Exception ex) {
            //just one is inserted
            assertPersistManyWithNullVersionAndSameId(carV, carV2);
            try {
                carVRepository.persist(List.of(carV, carV2)).await().indefinitely();
            } catch (Exception ignored) {
                assertPersistManyWithNullVersionAndSameId(carV, carV2);
            }
        }
    }

    private void assertPersistManyWithNullVersionAndSameId(CarV carV, CarV carV2) {
        if (carV.version != null) {
            assertEquals(0L, carV.version); //inserted
            assertNull(carV2.version); //failed
        } else {
            assertEquals(0L, carV2.version); //inserted
            assertNull(carV.version); //failed
        }
    }

    /**
     * Try to persist two documents with generatedId.
     */
    @Test
    public void persistManyWithNullVersionAndAutoGeneratedId() {
        BikeV bikeV = new BikeV(null, null);
        BikeV bikeV2 = new BikeV(null, null);

        BikeV bikeV3 = null; //will use bikeV id
        try {
            bikeVRepository.persist(List.of(bikeV, bikeV2)).await().indefinitely();
            assertEquals(0L, bikeV.version); //inserted
            assertEquals(0L, bikeV2.version); //inserted

            bikeV3 = new BikeV(bikeV.id, null);
            bikeVRepository.persist(List.of(bikeV, bikeV2, bikeV3)).await().indefinitely();
        } catch (Exception ignored) {
            assertEquals(0L, bikeV.version); //didn't change
            assertEquals(0L, bikeV2.version); //didn't change
            assertNull(bikeV3.version); //failed
        }
    }

    /**
     * Mix of inserts and updates, keeping the version untouched for failed.
     */
    @Test
    public void persistOrUpdateManyWithVersion() {
        String id = UUID.randomUUID().toString();

        //persist one to have some to update
        carVRepository.persistOrUpdate(new CarV(id, 2022, null)).await().indefinitely();

        CarV carV = new CarV("carv", 2022, null);
        CarV carV2 = new CarV(id, 2022, 0L);
        CarV carV3 = new CarV(id, 2023, 5L);
        CarV carV4 = new CarV("carv4", 2022, 0L);

        try {
            carVRepository.persistOrUpdate(List.of(carV, carV2, carV3, carV4)).await().indefinitely();
        } catch (Exception ex) {
            assertEquals(0, carV.version); //inserted
            //since there is no order guaranteed because the combine() we check which one was updated
            if (1L == carV2.version) {
                assertEquals(1L, carV2.version); //updated
                assertEquals(5L, carV3.version); //failed
            } else {
                assertEquals(null, carV2.version); //failed
                assertEquals(6L, carV3.version); //updated
            }

            assertEquals(0L, carV4.version); //failed
            try {
                carVRepository.persistOrUpdate(List.of(carV, carV2, carV3, carV4)).await().indefinitely();
            } catch (Exception ignored) {
            }
        } finally {
            assertEquals(1L, carV.version); //updated in catch

            if (2L == carV2.version) {
                assertEquals(2L, carV2.version); //updated
                assertEquals(5L, carV3.version); //failed
            } else {
                assertEquals(null, carV2.version); //failed
                assertEquals(7L, carV3.version); //updated
            }
            assertEquals(0L, carV4.version); //failed
        }
    }

    /**
     * Mix of inserts and updates, keeping the version untouched for failed.
     */
    @Test
    public void persistOrUpdateManyWithVersionAndAutoGeneratedId() {
        //persist one to have some to update
        var bikeVPersisted = new BikeV(null, null);
        bikeVRepository.persistOrUpdate(bikeVPersisted).await().indefinitely();

        BikeV bikeV = new BikeV(null, null);
        BikeV bikeV2 = new BikeV(bikeVPersisted.id, 0L);
        BikeV bikeV3 = new BikeV(null, null);
        BikeV bikeV4 = new BikeV(bikeVPersisted.id, 10L);

        try {
            bikeVRepository.persistOrUpdate(List.of(bikeV, bikeV2, bikeV3, bikeV4)).await().indefinitely();
        } catch (Exception ex) {
            assertEquals(0, bikeVPersisted.version); //didn't change
            assertEquals(1L, bikeV2.version); //updated
            assertEquals(0L, bikeV3.version); //inserted
            assertEquals(10L, bikeV4.version); //failed
            try {
                bikeVRepository.persistOrUpdate(List.of(bikeV, bikeV2, bikeV3, bikeV4)).await().indefinitely();
            } catch (Exception ignored) {
                assertEquals(0, bikeVPersisted.version); //didn't change
                assertEquals(2L, bikeV2.version); //updated
                assertEquals(1L, bikeV3.version); //inserted
                assertEquals(10L, bikeV4.version); //failed
            }
        }
    }

    @Test
    public void deleteWithoutVersionNonExistentDocument() {
        Car car = buildDefault();
        carRepository.delete(car).await().indefinitely();
    }

    @Test
    public void deleteById() {
        var carV = buildDefaultV();
        carVRepository.persist(carV).await().indefinitely();
        carVRepository.deleteById(carV.id).await().indefinitely();

        assertTrue(carVRepository.findAll().list().await().indefinitely().isEmpty());
    }

    private CarV buildDefaultV() {
        return new CarV(UUID.randomUUID().toString(), 2022, null);
    }

    private Car buildDefault() {
        return new Car(UUID.randomUUID().toString(), 2022);
    }

    @ApplicationScoped
    public static class CarRepository implements ReactivePanacheMongoRepositoryBase<Car, String> {

    }

    @ApplicationScoped
    public static class CarVRepository implements ReactivePanacheMongoRepositoryBase<CarV, String> {

    }

    @ApplicationScoped
    public static class BikeVRepository implements ReactivePanacheMongoRepositoryBase<BikeV, String> {

    }
}
