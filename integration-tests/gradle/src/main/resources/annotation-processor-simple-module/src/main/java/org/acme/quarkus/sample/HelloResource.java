package org.acme.quarkus.sample;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.acme.quarkus.CarMapper;
import org.acme.quarkus.domain.Car;
import org.acme.quarkus.domain.CarDTO;

@Path("/hello")
public class HelloResource {

    private final CarMapper carMapper;

    public HelloResource(CarMapper carMapper) {
        this.carMapper = carMapper;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public CarDTO hello() {
        Car car = new Car("foo", 4);
        return carMapper.carToCarDTO(car);
    }
}