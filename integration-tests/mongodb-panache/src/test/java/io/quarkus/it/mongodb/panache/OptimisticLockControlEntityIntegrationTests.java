package io.quarkus.it.mongodb.panache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

import io.quarkus.it.mongodb.panache.model.CarEntity;
import io.quarkus.it.mongodb.panache.model.CarVEntity;
import io.quarkus.mongodb.panache.common.exception.OptimisticLockException;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class OptimisticLockControlEntityIntegrationTests {

    @BeforeEach
    public void beforeEach() {
        CarEntity.deleteAll();
        CarVEntity.deleteAll();
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
                .filter(entry -> entry.getValue())
                .map(entry -> entry.getKey())
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
                .mapToObj(i -> new CarVEntity(id, i, 0l))
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
                .filter(entry -> entry.getValue())
                .map(entry -> entry.getKey())
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

    @Test
    public void persistOrUpdateManyWithVersion() {
        String id = UUID.randomUUID().toString();
        CarVEntity.persist(new CarVEntity(id, 2021, null));

        List<CarVEntity> carList = IntStream.range(0, 1)
                .mapToObj(i -> new CarVEntity(id, i, 0l))
                .collect(Collectors.toList());
        CarVEntity.persistOrUpdate(carList);

        List<CarVEntity> carDbList = CarVEntity.findAll().list();
        assertEquals(1, carDbList.size());
        assertEquals(0, carDbList.get(0).modelYear);
        assertEquals(1l, carDbList.get(0).version);
    }

    @Test
    public void deleteWithoutVersionNonExistentDocument() {
        CarEntity car = buildDefault();
        car.delete();
    }

    @Test
    public void deleteWithVersionNonExistentDocument() {
        CarVEntity car = buildDefaultV();
        assertThrows(OptimisticLockException.class, () -> car.delete());
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
