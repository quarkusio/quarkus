package io.quarkus.it.mongodb.panache;

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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mongodb.MongoWriteException;

import io.quarkus.it.mongodb.panache.model.BikeV;
import io.quarkus.it.mongodb.panache.model.BikeVEntity;
import io.quarkus.it.mongodb.panache.model.CarEntity;
import io.quarkus.it.mongodb.panache.model.CarV;
import io.quarkus.it.mongodb.panache.model.CarVEntity;
import io.quarkus.mongodb.panache.common.exception.OptimisticLockException;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class OptimisticLockControlEntityIntegrationTests {

    @BeforeEach
    public void beforeEach() {
        CarEntity.deleteAll();
        CarVEntity.deleteAll();
        BikeVEntity.deleteAll();
    }

    @Test
    public void persistOneWithoutVersion() {
        CarEntity carWithoutVersion = buildDefault();
        CarEntity.persist(carWithoutVersion);

        CarEntity carDb = CarEntity.findById(carWithoutVersion.id);
        assertNotNull(carDb);
        assertEquals(carWithoutVersion, carDb);
    }

    @Test
    public void persistOneWithVersion() {
        CarVEntity carWithVersion = buildDefaultV();
        CarVEntity.persist(carWithVersion);

        CarVEntity carDb = CarVEntity.findById(carWithVersion.id);
        assertNotNull(carDb);
        assertEquals(0, carDb.version);
    }

    @Test
    public void updateOneWithoutVersion() {
        CarEntity carWithoutVersion = buildDefault();
        CarEntity.persist(carWithoutVersion);

        CarEntity carDb = CarEntity.findById(carWithoutVersion.id);
        assertNotNull(carDb);
        assertEquals(carWithoutVersion, carDb);

        carWithoutVersion = new CarEntity(carWithoutVersion.id, 2022);
        CarEntity.update(carWithoutVersion);

        carDb = CarEntity.findById(carWithoutVersion.id);
        assertNotNull(carDb);
        assertEquals(carWithoutVersion, carDb);
    }

    @Test
    public void updateOneWithVersion() {
        CarVEntity car = buildDefaultV();
        CarVEntity.persist(car);

        CarVEntity carDb = CarVEntity.findById(car.id);
        assertEquals(0, carDb.version);

        car = new CarVEntity(carDb.id, 2022, carDb.version);
        CarVEntity.update(car);

        carDb = CarVEntity.findById(car.id);
        assertEquals(1, carDb.version);
    }

    @Test
    public void updateOneWithOutdatedVersion() {
        CarVEntity car = buildDefaultV();
        CarVEntity.persist(car);

        CarVEntity carDb = CarVEntity.findById(car.id);
        assertNotNull(carDb);
        assertEquals(car, carDb);

        OptimisticLockException optimisticLockException = assertThrows(OptimisticLockException.class, () -> {
            CarVEntity car2 = new CarVEntity(carDb.id, 2021, -1l);
            CarVEntity.update(car2);
        });

        assertNotNull(optimisticLockException.getMessage());
    }

    @Test
    public void updateOneNonExistingWithVersion() {
        CarVEntity car = buildDefaultV();
        CarVEntity.persist(car);

        CarVEntity carDb = CarVEntity.findById(car.id);
        assertEquals(0, carDb.version);

        assertThrows(OptimisticLockException.class, () -> {
            CarVEntity carV = new CarVEntity(UUID.randomUUID().toString(), 2022, null);
            CarVEntity.update(carV);
        });

        carDb = CarVEntity.findById(car.id);
        assertEquals(0, carDb.version);
    }

    @Test
    public void persistOrUpdateOneWithoutVersion() {
        CarEntity carWithoutVersion = buildDefault();
        CarEntity.persistOrUpdate(carWithoutVersion);

        CarEntity carDb = CarEntity.findById(carWithoutVersion.id);
        assertNotNull(carDb);
        assertEquals(carWithoutVersion, carDb);

        carWithoutVersion = new CarEntity(carWithoutVersion.id, 2022);
        CarEntity.persistOrUpdate(carWithoutVersion);

        carDb = CarEntity.findById(carWithoutVersion.id);
        assertNotNull(carDb);
        assertEquals(carWithoutVersion, carDb);
    }

    @Test
    public void persistOrUpdateOneWithVersion() {
        //add the car
        CarVEntity car = buildDefaultV();
        CarVEntity.persistOrUpdate(car);
        CarVEntity carDb = CarVEntity.findById(car.id);
        assertEquals(0, carDb.version);

        //must update the car
        car = new CarVEntity(carDb.id, 2022, carDb.version);
        CarVEntity.persistOrUpdate(car);

        carDb = CarVEntity.findById(car.id);
        assertEquals(1, carDb.version);
        assertEquals(2022, carDb.modelYear);
    }

    @Test
    public void persistOrUpdateOneWithOutdatedVersion() {
        //persist the car
        CarVEntity car = buildDefaultV();
        CarVEntity.persistOrUpdate(car);
        CarVEntity carDb = CarVEntity.findById(car.id);
        assertNotNull(carDb);
        assertEquals(car, carDb);

        //fail trying to update without version
        MongoWriteException mongoWriteException = assertThrows(MongoWriteException.class, () -> {
            CarVEntity car2 = new CarVEntity(carDb.id, 2021, null);
            CarVEntity.persistOrUpdate(car2);
        });

        assertTrue(mongoWriteException.getMessage().contains("E11000 duplicate key error collection"));
    }

    @Test
    public void persistOrUpdateOneConcurrentReqs() {
        Map<Integer, Boolean> mapStatus = new HashMap<>();
        String id = UUID.randomUUID().toString();
        CarVEntity carV = new CarVEntity(id, 2021, null);
        CarVEntity.persist(carV);

        List<CarVEntity> carVListToUpdate = IntStream.range(0, 10)
                .mapToObj(i -> new CarVEntity(id, i, 0l))
                .collect(Collectors.toList());

        carVListToUpdate.parallelStream()
                .forEach(carV1 -> {
                    try {
                        CarVEntity.persistOrUpdate(carV1);
                        mapStatus.put(carV1.modelYear, true);
                    } catch (OptimisticLockException ex) {
                        mapStatus.put(carV1.modelYear, false);
                    }
                });

        List<CarVEntity> carVList2 = CarVEntity.findAll().list();
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
        CarVEntity carV = new CarVEntity(id, 2021, null);
        CarVEntity.persist(carV);

        List<CarVEntity> carVListToUpdate = IntStream.range(0, 10)
                .mapToObj(i -> new CarVEntity(id, i, 0L))
                .collect(Collectors.toList());

        carVListToUpdate.parallelStream()
                .forEach(carV1 -> {
                    try {
                        CarVEntity.update(carV1);
                        mapStatus.put(carV1.modelYear, true);
                    } catch (OptimisticLockException ex) {
                        mapStatus.put(carV1.modelYear, false);
                    }
                });

        List<CarVEntity> carVList2 = CarVEntity.findAll().list();
        assertEquals(1, carVList2.size());

        List<Integer> updateWithSuccessList = mapStatus.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        assertEquals(1, updateWithSuccessList.size());
        assertEquals(updateWithSuccessList.get(0), carVList2.get(0).modelYear);
    }

    @Test
    public void prsistManyWithoutVersion() {
        List<CarEntity> carList = IntStream.range(0, 10)
                .mapToObj(i -> new CarEntity(UUID.randomUUID().toString(), i))
                .collect(Collectors.toList());
        CarEntity.persist(carList);

        List<CarEntity> carDbList = CarEntity.findAll().list();
        assertEquals(10, carDbList.size());
    }

    @Test
    public void persistManyWithVersion() {
        List<CarVEntity> carList = IntStream.range(0, 10)
                .mapToObj(i -> new CarVEntity(UUID.randomUUID().toString(), i, null))
                .collect(Collectors.toList());
        CarVEntity.persist(carList);

        List<CarVEntity> carDbList = CarVEntity.findAll().list();
        assertEquals(10, carDbList.size());
        carDbList.forEach(carV -> assertEquals(0l, carV.version));
    }

    @Test
    public void UpdateManyWithoutVersion() {
        String id = UUID.randomUUID().toString();
        CarEntity.persist(new CarEntity(id, 2021));

        List<CarEntity> carList = IntStream.range(0, 10)
                .mapToObj(i -> new CarEntity(id, i))
                .collect(Collectors.toList());
        CarEntity.update(carList);

        List<CarEntity> carDbList = CarEntity.findAll().list();
        assertEquals(1, carDbList.size());
    }

    @Test
    public void updateManyWithVersion() {
        //add one
        String id = UUID.randomUUID().toString();
        CarVEntity.persist(new CarVEntity(id, 2021, null));

        //update this one and check the values
        CarVEntity.update(Arrays.asList(new CarVEntity(id, 0, 0l)));

        List<CarVEntity> carDbList = CarVEntity.findAll().list();
        assertEquals(1, carDbList.size());
        CarVEntity carV = carDbList.get(0);
        assertEquals(0, carV.modelYear);
        assertEquals(1l, carV.version);

        //update again
        CarVEntity.update(Arrays.asList(new CarVEntity(id, 2, 1l)));
        carDbList = CarVEntity.findAll().list();
        assertEquals(1, carDbList.size());
        carV = carDbList.get(0);
        assertEquals(2, carV.modelYear);
        assertEquals(2l, carV.version);

        //try to update with wrong version
        assertThrows(OptimisticLockException.class,
                () -> CarVEntity.update(Arrays.asList(new CarVEntity(id, 1, 1l))));

        //update using a list containing more elements, must fail
        List<CarVEntity> carList = IntStream.range(0, 10)
                .mapToObj(i -> new CarVEntity(id, i, 2l))
                .collect(Collectors.toList());
        assertThrows(OptimisticLockException.class, () -> CarVEntity.update(carList));
    }

    @Test
    public void persistOrUpdateManyWithoutVersion() {
        List<CarEntity> carList = IntStream.range(0, 10)
                .mapToObj(i -> new CarEntity(UUID.randomUUID().toString(), i))
                .collect(Collectors.toList());
        CarEntity.persistOrUpdate(carList);

        List<CarEntity> carDbList = CarEntity.findAll().list();
        assertEquals(10, carDbList.size());
    }

    /**
     * Try to persist two documents with the same id.
     */
    @Test
    public void persistManylWithNullVersionAndSameId() {
        String id = UUID.randomUUID().toString();
        CarV carV = new CarV(id, 2022, null);
        CarV carV2 = new CarV(id, 2022, null);

        try {
            //try to insert two with same id
            CarEntity.persist(List.of(carV, carV2));
        } catch (Exception ex) {
            assertEquals(0L, carV.version); //inserted
            assertNull(carV2.version); //failed
            try {
                CarEntity.persist(List.of(carV, carV2));
            } catch (Exception ignored) {
            }
        } finally {
            assertEquals(0L, carV.version); //didn't change
            assertNull(carV2.version); //failed
        }
    }

    /**
     * Try to persist two documents with generatedId.
     */
    @Test
    public void persistManylWithNullVersionAndAutoGeneratedId() {
        BikeV bikeV = new BikeV(null, null);
        BikeV bikeV2 = new BikeV(null, null);

        BikeV bikeV3 = null; //will use bikeV id
        try {
            BikeVEntity.persist(List.of(bikeV, bikeV2));
            assertEquals(0L, bikeV.version); //inserted
            assertEquals(0L, bikeV2.version); //inserted

            bikeV3 = new BikeV(bikeV.id, null);
            BikeVEntity.persist(List.of(bikeV, bikeV2, bikeV3));
        } catch (Exception ignored) {

        } finally {
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
        BikeVEntity.persistOrUpdate(new CarV(id, 2022, null));

        CarV carV = new CarV("carv", 2022, null);
        CarV carV2 = new CarV(id, 2022, 0L);
        CarV carV3 = new CarV(id, 2023, 5L);
        CarV carV4 = new CarV("carv4", 2022, 0L);

        System.out.println(" c: " + carV);
        System.out.println(" c: " + carV2);
        System.out.println(" c: " + carV3);
        System.out.println(" c: " + carV4);

        try {
            BikeVEntity.persistOrUpdate(List.of(carV, carV2, carV3, carV4));
        } catch (Exception ex) {
            System.out.println(" c1: " + carV);
            System.out.println(" c1: " + carV2);
            System.out.println(" c1: " + carV3);
            System.out.println(" c1: " + carV4);

            assertEquals(0, carV.version); //inserted
            assertEquals(1L, carV2.version); //updated
            assertEquals(5L, carV3.version); //failed
            assertEquals(0L, carV4.version); //failed

            System.out.println(" c2: " + carV);
            System.out.println(" c2: " + carV2);
            System.out.println(" c2: " + carV3);
            System.out.println(" c2: " + carV4);
            try {
                BikeVEntity.persistOrUpdate(List.of(carV, carV2, carV3, carV4));
            } catch (Exception ignored) {
            }
        } finally {
            System.out.println(" c3: " + carV);
            System.out.println(" c3: " + carV2);
            System.out.println(" c3: " + carV3);
            System.out.println(" c3: " + carV4);
            assertEquals(1L, carV.version); //updated in catch
            assertEquals(2L, carV2.version); //updated in catch
            assertEquals(5L, carV3.version); //failed
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
        BikeVEntity.persistOrUpdate(bikeVPersisted);

        BikeV bikeV = new BikeV(null, null);
        BikeV bikeV2 = new BikeV(bikeVPersisted.id, 0L);
        BikeV bikeV3 = new BikeV(null, null);
        BikeV bikeV4 = new BikeV(bikeVPersisted.id, 10L);

        try {
            BikeVEntity.persistOrUpdate(List.of(bikeV, bikeV2, bikeV3, bikeV4));
        } catch (Exception ex) {
            assertEquals(0, bikeVPersisted.version); //didn't change
            assertEquals(1L, bikeV2.version); //updated
            assertEquals(0L, bikeV3.version); //inserted
            assertEquals(10L, bikeV4.version); //failed
            try {
                BikeVEntity.persistOrUpdate(List.of(bikeV, bikeV2, bikeV3, bikeV4));
            } catch (Exception ignored) {
            }
        } finally {
            assertEquals(0, bikeVPersisted.version); //didn'd change
            assertEquals(2L, bikeV2.version); //updated
            assertEquals(1L, bikeV3.version); //inserted
            assertEquals(10L, bikeV4.version); //failed
        }
    }

    @Test
    public void deleteWithoutVersionNonExistentDocument() {
        CarEntity car = buildDefault();
        car.delete();
    }

    @Test
    public void deleteById() {
        var carV = buildDefaultV();
        CarVEntity.persist(carV);
        CarVEntity.deleteById(carV.id);

        assertTrue(CarVEntity.findAll().list().isEmpty());
    }

    private CarVEntity buildDefaultV() {
        return new CarVEntity(UUID.randomUUID().toString(), 2022, null);
    }

    private CarEntity buildDefault() {
        return new CarEntity(UUID.randomUUID().toString(), 2022);
    }
}
