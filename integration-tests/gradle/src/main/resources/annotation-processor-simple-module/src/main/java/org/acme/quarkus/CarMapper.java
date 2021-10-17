package org.acme.quarkus;

import org.acme.quarkus.domain.Car;
import org.acme.quarkus.domain.CarDTO;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "cdi", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface CarMapper {

    @Mapping(source = "numberOfSeats", target = "seatNumber")
    CarDTO carToCarDTO(Car car);

}