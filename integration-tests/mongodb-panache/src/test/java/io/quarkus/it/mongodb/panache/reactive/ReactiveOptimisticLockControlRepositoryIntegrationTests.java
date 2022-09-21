package io.quarkus.it.mongodb.panache.reactive;

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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mongodb.MongoWriteException;

import io.quarkus.it.mongodb.panache.model.Car;
import io.quarkus.it.mongodb.panache.model.CarV;
import io.quarkus.mongodb.panache.common.exception.OptimisticLockException;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepositoryBase;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ReactiveOptimisticLockControlRepositoryIntegrationTests {

    @Inject
    CarRepository carRepository;

    @Inject
    CarVRepository carVRepository;

    @BeforeEach
    public void beforeEach() {
        carRepository.deleteAll().await().indefinitely();
        carVRepository.deleteAll().await().indefinitely();
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
        carVRepository.update(Arrays.asList(new CarV(id, 2, 1L))).await().indefinitely();
        carDbList = carVRepository.findAll().list().await().indefinitely();
        assertEquals(1, carDbList.size());
        carV = carDbList.get(0);
        assertEquals(2, carV.modelYear);
        assertEquals(2L, carV.version);

        //try to update with wrong version
        assertThrows(OptimisticLockException.class,
                () -> carVRepository.update(Arrays.asList(new CarV(id, 1, 1L))).await().indefinitely());

        //update using a list containing more elements, must fail
        List<CarV> carList = IntStream.range(0, 10)
                .mapToObj(i -> new CarV(id, i, 2L))
                .collect(Collectors.toList());
        assertThrows(OptimisticLockException.class, () -> carVRepository.update(carList).await().indefinitely());
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

    @Test
    public void persistOrUpdateManyWithVersion() {
        String id = UUID.randomUUID().toString();
        carVRepository.persist(new CarV(id, 2021, null)).await().indefinitely();

        List<CarV> carList = IntStream.range(0, 1)
                .mapToObj(i -> new CarV(id, i, 0l))
                .collect(Collectors.toList());
        carVRepository.persistOrUpdate(carList).await().indefinitely();

        List<CarV> carDbList = carVRepository.findAll().list().await().indefinitely();
        assertEquals(1, carDbList.size());
        assertEquals(0, carDbList.get(0).modelYear);
        assertEquals(1L, carDbList.get(0).version);
    }

    @Test
    public void deleteWithoutVersionNonExistentDocument() {
        Car car = buildDefault();
        carRepository.delete(car).await().indefinitely();
    }

    @Test
    public void deleteWithVersionNonExistentDocument() {
        CarV car = buildDefaultV();
        assertThrows(OptimisticLockException.class, () -> carVRepository.delete(car).await().indefinitely());
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
}
