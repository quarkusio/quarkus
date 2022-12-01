package org.acme.common;

import org.acme.common.domain.Car;
import org.acme.common.domain.CarDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface CarMapper {

    CarMapper INSTANCE = Mappers.getMapper( CarMapper.class );

    @Mapping(source = "numberOfSeats", target = "seatNumber")
    CarDTO carToCarDTO(Car car);

}