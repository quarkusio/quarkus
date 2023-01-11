package io.quarkus.it.mongodb.panache.model

import io.quarkus.mongodb.panache.kotlin.PanacheMongoEntityBase

abstract class VehicleEntity(open var id: String, open var modelYear: Int?) : PanacheMongoEntityBase()
