package io.quarkus.it.mongodb.panache.reactive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import io.quarkus.it.mongodb.panache.model.ReactiveCarEntity;
import io.quarkus.it.mongodb.panache.model.ReactiveCarVEntity;
import io.quarkus.it.mongodb.panache.model.ReactiveVehicleEntity;
import io.quarkus.mongodb.panache.common.exception.OptimisticLockException;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ReactiveOptimisticLockControlEntityIntegrationTests {

    @BeforeEach
    public void beforeEach() {
        ReactiveCarEntity.deleteAll().await().indefinitely();
        ReactiveCarVEntity.deleteAll().await().indefinitely();
    }

    @Test
    public void persistOneWithoutVersion() {
        ReactiveCarEntity carWithoutVersion = buildDefault();
        CarEntity.persist(carWithoutVersion);

        ReactiveCarEntity carDb = ReactiveCarEntity.findById(carWithoutVersion.id)
                .onItem()
                .transform(e -> (ReactiveCarEntity) e)
                .await().indefinitely();
        assertNotNull(carDb);
        assertEquals(carWithoutVersion, carDb);
    }

    @Test
    public void persistOneWithVersion() {
        ReactiveCarVEntity carWithVersion = buildDefaultV();
        ReactiveCarVEntity.persist(carWithVersion).await().indefinitely();

        ReactiveCarVEntity carDb = ReactiveCarVEntity.findById(carWithVersion.id)
                .onItem()
                .transform(e -> (ReactiveCarVEntity) e)
                .await().indefinitely();
        assertNotNull(carDb);
        assertEquals(0, carDb.version);
    }

    @Test
    public void updateOneWithoutVersion() {
        ReactiveCarEntity carWithoutVersion = buildDefault();
        ReactiveCarEntity.persist(carWithoutVersion).await().indefinitely();

        ReactiveCarEntity carDb = ReactiveCarEntity.findById(carWithoutVersion.id)
                .onItem()
                .transform(e -> (ReactiveCarEntity) e)
                .await().indefinitely();
        assertNotNull(carDb);
        assertEquals(carWithoutVersion, carDb);

        carWithoutVersion = new ReactiveCarEntity(carWithoutVersion.id, 2022);
        ReactiveCarEntity.update(carWithoutVersion).await().indefinitely();

        carDb = ReactiveCarEntity.findById(carWithoutVersion.id)
                .onItem()
                .transform(e -> (ReactiveCarEntity) e)
                .await().indefinitely();
        assertNotNull(carDb);
        assertEquals(carWithoutVersion, carDb);
    }

    @Test
    public void updateOneWithVersion() {
        ReactiveCarVEntity car = buildDefaultV();
        ReactiveCarVEntity.persist(car).await().indefinitely();

        ReactiveCarVEntity carDb = ReactiveCarVEntity.findById(car.id)
                .onItem()
                .transform(e -> (ReactiveCarVEntity) e)
                .await().indefinitely();
        assertEquals(0, carDb.version);

        car = new ReactiveCarVEntity(carDb.id, 2022, carDb.version);
        ReactiveCarVEntity.update(car).await().indefinitely();

        carDb = ReactiveCarVEntity.findById(car.id)
                .onItem()
                .transform(e -> (ReactiveCarVEntity) e)
                .await().indefinitely();
        ;
        assertEquals(1, carDb.version);
    }

    @Test
    public void updateOneWithOutdatedVersion() {
        ReactiveCarVEntity car = buildDefaultV();
        ReactiveCarVEntity.persist(car).await().indefinitely();

        ReactiveCarVEntity carDb = ReactiveCarVEntity.findById(car.id)
                .onItem()
                .transform(e -> (ReactiveCarVEntity) e)
                .await().indefinitely();
        assertNotNull(carDb);
        assertEquals(car, carDb);

        OptimisticLockException optimisticLockException = assertThrows(OptimisticLockException.class, () -> {
            ReactiveCarVEntity car2 = new ReactiveCarVEntity(carDb.id, 2021, -1L);
            ReactiveCarVEntity.update(car2).await().indefinitely();
        });

        assertNotNull(optimisticLockException.getMessage());
    }

    @Test
    public void updateOneNonExistingWithVersion() {
        ReactiveCarVEntity car = buildDefaultV();
        ReactiveCarVEntity.persist(car).await().indefinitely();

        ReactiveCarVEntity carDb = ReactiveCarVEntity.findById(car.id)
                .onItem()
                .transform(e -> (ReactiveCarVEntity) e)
                .await().indefinitely();
        ;
        assertEquals(0, carDb.version);

        assertThrows(OptimisticLockException.class, () -> {
            ReactiveCarVEntity carV = new ReactiveCarVEntity(UUID.randomUUID().toString(), 2022, null);
            ReactiveCarVEntity.update(carV).await().indefinitely();
        });

        carDb = ReactiveCarVEntity.findById(car.id)
                .onItem()
                .transform(e -> (ReactiveCarVEntity) e)
                .await().indefinitely();
        ;
        assertEquals(0, carDb.version);
    }

    @Test
    public void persistOrUpdateOneWithoutVersion() {
        ReactiveCarEntity carWithoutVersion = buildDefault();
        ReactiveCarEntity.persistOrUpdate(carWithoutVersion).await().indefinitely();

        ReactiveCarEntity carDb = ReactiveCarEntity.findById(carWithoutVersion.id)
                .onItem()
                .transform(e -> (ReactiveCarEntity) e)
                .await().indefinitely();
        assertNotNull(carDb);
        assertEquals(carWithoutVersion, carDb);

        carWithoutVersion = new ReactiveCarEntity(carWithoutVersion.id, 2022);
        CarEntity.persistOrUpdate(carWithoutVersion);

        carDb = ReactiveCarEntity.findById(carWithoutVersion.id)
                .onItem()
                .transform(e -> (ReactiveCarEntity) e)
                .await().indefinitely();
        assertNotNull(carDb);
        assertEquals(carWithoutVersion, carDb);
    }

    @Test
    public void persistOrUpdateOneWithVersion() {
        //add the car
        ReactiveVehicleEntity car = buildDefaultV();
        ReactiveCarVEntity.persistOrUpdate(car).await().indefinitely();
        ReactiveCarVEntity carDb = ReactiveCarVEntity.findById(car.id)
                .onItem()
                .transform(e -> (ReactiveCarVEntity) e)
                .await().indefinitely();
        ;
        assertEquals(0, carDb.version);

        //must update the car
        car = new ReactiveCarVEntity(carDb.id, 2022, carDb.version);
        ReactiveCarVEntity.persistOrUpdate(car).await().indefinitely();

        carDb = ReactiveCarVEntity.findById(car.id)
                .onItem()
                .transform(e -> (ReactiveCarVEntity) e)
                .await().indefinitely();
        ;
        assertEquals(1, carDb.version);
        assertEquals(2022, carDb.modelYear);
    }

    @Test
    public void persistOrUpdateOneWithOutdatedVersion() {
        //persist the car
        ReactiveCarVEntity car = buildDefaultV();
        ReactiveCarVEntity.persistOrUpdate(car).await().indefinitely();
        ReactiveCarVEntity carDb = ReactiveCarVEntity.findById(car.id)
                .onItem()
                .transform(e -> (ReactiveCarVEntity) e)
                .await().indefinitely();
        ;
        assertNotNull(carDb);
        assertEquals(car, carDb);

        //fail trying to update without version
        MongoWriteException mongoWriteException = assertThrows(MongoWriteException.class, () -> {
            ReactiveCarVEntity car2 = new ReactiveCarVEntity(carDb.id, 2021, null);
            ReactiveCarVEntity.persistOrUpdate(car2).await().indefinitely();
        });

        assertTrue(mongoWriteException.getMessage().contains("E11000 duplicate key error collection"));
    }

    @Test
    public void persistOrUpdateOneConcurrentReqs() {
        Map<Integer, Boolean> mapStatus = new HashMap<>();
        String id = UUID.randomUUID().toString();
        ReactiveCarVEntity carV = new ReactiveCarVEntity(id, 2021, null);
        ReactiveCarVEntity.persist(carV).await().indefinitely();

        List<ReactiveCarVEntity> carVListToUpdate = IntStream.range(0, 10)
                .mapToObj(i -> new ReactiveCarVEntity(id, i, 0L))
                .collect(Collectors.toList());

        carVListToUpdate.parallelStream()
                .forEach(carV1 -> {
                    try {
                        ReactiveCarVEntity.persistOrUpdate(carV1).await().indefinitely();
                        mapStatus.put(carV1.modelYear, true);
                    } catch (OptimisticLockException ex) {
                        mapStatus.put(carV1.modelYear, false);
                    }
                });

        List<ReactivePanacheMongoEntityBase> carVList2 = ReactiveCarVEntity.findAll().list()
                .await().indefinitely();
        assertEquals(1, carVList2.size());

        List<Integer> updateWithSuccessList = mapStatus.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        assertEquals(1, updateWithSuccessList.size());
        assertEquals(updateWithSuccessList.get(0), ((ReactiveCarVEntity) carVList2.get(0)).modelYear);
    }

    @Test
    public void updateOneConcurrentReqs() {
        Map<Integer, Boolean> mapStatus = new HashMap<>();
        String id = UUID.randomUUID().toString();
        ReactiveCarVEntity carV = new ReactiveCarVEntity(id, 2021, null);
        ReactiveCarVEntity.persist(carV).await().indefinitely();

        List<ReactiveCarVEntity> carVListToUpdate = IntStream.range(0, 10)
                .mapToObj(i -> new ReactiveCarVEntity(id, i, 0L))
                .collect(Collectors.toList());

        carVListToUpdate.parallelStream()
                .forEach(carV1 -> {
                    try {
                        ReactiveCarVEntity.update(carV1).await().indefinitely();
                        mapStatus.put(carV1.modelYear, true);
                    } catch (OptimisticLockException ex) {
                        mapStatus.put(carV1.modelYear, false);
                    }
                });

        List<ReactivePanacheMongoEntityBase> carVList2 = ReactiveCarVEntity.findAll().list()
                .await().indefinitely();
        assertEquals(1, carVList2.size());

        List<Integer> updateWithSuccessList = mapStatus.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        assertEquals(1, updateWithSuccessList.size());
        assertEquals(updateWithSuccessList.get(0), ((ReactiveCarVEntity) carVList2.get(0)).modelYear);
    }

    @Test
    public void persistManyWithoutVersion() {
        List<ReactiveCarEntity> carList = IntStream.range(0, 10)
                .mapToObj(i -> new ReactiveCarEntity(UUID.randomUUID().toString(), i))
                .collect(Collectors.toList());
        ReactiveCarEntity.persist(carList).await().indefinitely();

        List<ReactivePanacheMongoEntityBase> carDbList = ReactiveCarEntity.findAll().list()
                .await().indefinitely();
        assertEquals(10, carDbList.size());
    }

    @Test
    public void persistManyWithVersion() {
        List<ReactiveCarVEntity> carList = IntStream.range(0, 10)
                .mapToObj(i -> new ReactiveCarVEntity(UUID.randomUUID().toString(), i, null))
                .collect(Collectors.toList());
        ReactiveCarVEntity.persist(carList).await().indefinitely();

        List<ReactivePanacheMongoEntityBase> carDbList = ReactiveCarVEntity.findAll().list()
                .await().indefinitely();
        assertEquals(10, carDbList.size());
        carDbList.forEach(carV -> assertEquals(0L, ((ReactiveCarVEntity) carV).version));
    }

    @Test
    public void UpdateManyWithoutVersion() {
        String id = UUID.randomUUID().toString();
        ReactiveCarEntity.persist(new ReactiveCarEntity(id, 2021)).await().indefinitely();

        List<ReactiveCarEntity> carList = IntStream.range(0, 10)
                .mapToObj(i -> new ReactiveCarEntity(id, i))
                .collect(Collectors.toList());
        ReactiveCarEntity.update(carList).await().indefinitely();

        List<ReactivePanacheMongoEntityBase> carDbList = ReactiveCarEntity.findAll().list()
                .await().indefinitely();
        assertEquals(1, carDbList.size());
    }

    @Test
    public void updateManyWithVersion() {
        //add one
        String id = UUID.randomUUID().toString();
        ReactiveCarVEntity.persist(new ReactiveCarVEntity(id, 2021, null)).await().indefinitely();

        //update this one and check the values
        ReactiveCarVEntity.update(List.of(new ReactiveCarVEntity(id, 0, 0L))).await().indefinitely();

        List<ReactivePanacheMongoEntityBase> carDbList = ReactiveCarVEntity.findAll().list()
                .await().indefinitely();
        assertEquals(1, carDbList.size());
        ReactiveCarVEntity carV = (ReactiveCarVEntity) carDbList.get(0);
        assertEquals(0, carV.modelYear);
        assertEquals(1L, carV.version);

        //update again
        ReactiveCarVEntity.update(List.of(new ReactiveCarVEntity(id, 2, 1L))).await().indefinitely();
        carDbList = ReactiveCarVEntity.findAll().list().await().indefinitely();
        assertEquals(1, carDbList.size());
        carV = (ReactiveCarVEntity) carDbList.get(0);
        assertEquals(2, carV.modelYear);
        assertEquals(2L, carV.version);

        //try to update with wrong version
        assertThrows(OptimisticLockException.class,
                () -> ReactiveCarVEntity.update(List.of(new ReactiveCarVEntity(id, 1, 1L)))
                        .await().indefinitely());

        //update using a list containing more elements, must fail
        List<ReactiveCarVEntity> carList = IntStream.range(0, 10)
                .mapToObj(i -> new ReactiveCarVEntity(id, i, 2L))
                .collect(Collectors.toList());
        assertThrows(OptimisticLockException.class, () -> ReactiveCarVEntity.update(carList).await().indefinitely());
    }

    @Test
    public void persistOrUpdateManyWithoutVersion() {
        List<ReactiveCarEntity> carList = IntStream.range(0, 10)
                .mapToObj(i -> new ReactiveCarEntity(UUID.randomUUID().toString(), i))
                .collect(Collectors.toList());
        ReactiveCarEntity.persistOrUpdate(carList).await().indefinitely();

        List<ReactivePanacheMongoEntityBase> carDbList = ReactiveCarEntity.findAll().list()
                .await().indefinitely();
        assertEquals(10, carDbList.size());
    }

    @Test
    public void persistOrUpdateManyWithVersion() {
        String id = UUID.randomUUID().toString();
        ReactiveCarVEntity.persist(new ReactiveCarVEntity(id, 2021, null)).await().indefinitely();

        List<ReactiveCarVEntity> carList = IntStream.range(0, 1)
                .mapToObj(i -> new ReactiveCarVEntity(id, i, 0l))
                .collect(Collectors.toList());
        ReactiveCarVEntity.persistOrUpdate(carList).await().indefinitely();

        List<ReactivePanacheMongoEntityBase> carDbList = ReactiveCarVEntity.findAll().list().await().indefinitely();
        ReactiveCarVEntity carDB = (ReactiveCarVEntity) carDbList.get(0);
        assertEquals(1, carDbList.size());
        assertEquals(0, carDB.modelYear);
        assertEquals(1L, carDB.version);
    }

    @Test
    public void deleteWithoutVersionNonExistentDocument() {
        ReactiveCarEntity car = buildDefault();
        car.delete().await().indefinitely();
    }

    @Test
    public void deleteWithVersionNonExistentDocument() {
        ReactiveCarVEntity car = buildDefaultV();
        assertThrows(OptimisticLockException.class, () -> car.delete().await().indefinitely());
    }

    @Test
    public void deleteById() {
        var carV = buildDefaultV();
        ReactiveCarVEntity.persist(carV).await().indefinitely();
        ReactiveCarVEntity.deleteById(carV.id).await().indefinitely();

        assertTrue(ReactiveCarVEntity.findAll().list().await().indefinitely().isEmpty());
    }

    private ReactiveCarVEntity buildDefaultV() {
        return new ReactiveCarVEntity(UUID.randomUUID().toString(), 2022, null);
    }

    private ReactiveCarEntity buildDefault() {
        return new ReactiveCarEntity(UUID.randomUUID().toString(), 2022);
    }
}
