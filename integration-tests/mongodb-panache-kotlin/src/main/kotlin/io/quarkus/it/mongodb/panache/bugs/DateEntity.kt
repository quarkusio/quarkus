package io.quarkus.it.mongodb.panache.bugs

import io.quarkus.mongodb.panache.kotlin.PanacheMongoCompanion
import io.quarkus.mongodb.panache.kotlin.PanacheMongoEntity
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Date

/**
 * An entity that have all the supported date format. Asserting #6566 and possibility other date
 * issues.
 */
class DateEntity(
    var dateDate: Date = Date(),
    var localDate: LocalDate = LocalDate.now(),
    var localDateTime: LocalDateTime = LocalDateTime.now(),
    var instant: Instant = Instant.now(),
) : PanacheMongoEntity() {
    companion object : PanacheMongoCompanion<DateEntity>
}
