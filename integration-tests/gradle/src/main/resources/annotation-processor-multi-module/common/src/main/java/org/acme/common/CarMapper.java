package org.acme.common;

import org.acme.common.domain.Car;
import org.acme.common.domain.CarDTO;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "cdi", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface CarMapper {

    @Mapping(source = "numberOfSeats", target = "seatNumber")
    CarDTO carToCarDTO(Car car);

}