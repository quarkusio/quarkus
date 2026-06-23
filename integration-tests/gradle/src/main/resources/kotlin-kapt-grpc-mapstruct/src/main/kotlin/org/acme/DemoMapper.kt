package org.acme

import org.acme.HelloWorldProto
import org.mapstruct.Mapper

@Mapper
interface DemoMapper {
    fun map(request: HelloWorldProto.HelloRequest): HelloRequestModel
}
