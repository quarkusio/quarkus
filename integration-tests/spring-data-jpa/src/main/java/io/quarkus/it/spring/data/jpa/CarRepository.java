package io.quarkus.it.spring.data.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CarRepository extends JpaRepository<Car, Long> {

    List<Car> findByBrand(String brand);

    Car findByBrandAndModel(String brand, String model);

    @Query("SELECT m.model FROM MotorCar m WHERE m.brand = :brand")
    List<String> findModelsByBrand(@Param("brand") String brand);
}
