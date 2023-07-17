package org.acme.quarkus.sample;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.acme.common.CarMapper;
import org.acme.common.domain.Car;
import org.acme.common.domain.CarDTO;

@Path("/hello")
public class HelloResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public CarDTO hello() {
        Car car = new Car("foo", 4);
        return CarMapper.INSTANCE.carToCarDTO(car);
    }
}
