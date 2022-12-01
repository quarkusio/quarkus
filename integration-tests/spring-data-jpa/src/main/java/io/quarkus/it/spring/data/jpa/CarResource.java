package io.quarkus.it.spring.data.jpa;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

@Path("/car")
public class CarResource {

    private final CarRepository carRepository;

    public CarResource(CarRepository carRepository) {
        this.carRepository = carRepository;
    }

    @Path("brand/{brand}")
    @GET
    @Produces("application/json")
    public List<Car> carsByBrand(@PathParam("brand") String brand) {
        return carRepository.findByBrand(brand);
    }

    @Path("brand/{brand}/model/{model}")
    @GET
    @Produces("application/json")
    public Car findByBrandAndModel(@PathParam("brand") String brand, @PathParam("model") String model) {
        return carRepository.findByBrandAndModel(brand, model);
    }

    @Path("{id}")
    @GET
    @Produces("application/json")
    public Car carById(@PathParam("id") Long id) {
        return carRepository.findById(id).orElse(null);
    }

    @Path("brand/{brand}/models")
    @GET
    @Produces("application/json")
    public List<String> carModelsByBrand(@PathParam("brand") String brand) {
        return carRepository.findModelsByBrand(brand);
    }
}
