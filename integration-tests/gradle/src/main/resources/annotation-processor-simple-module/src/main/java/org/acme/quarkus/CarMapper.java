package org.acme.quarkus;

import org.acme.quarkus.domain.Car;
import org.acme.quarkus.domain.CarDTO;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface CarMapper {

    CarMapper INSTANCE = Mappers.getMapper( CarMapper.class );

    @Mapping(source = "numberOfSeats", target = "seatNumber")
    CarDTO carToCarDTO(Car car);

}