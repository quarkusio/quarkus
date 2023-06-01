package io.quarkus.it.resteasy.jackson;

import java.sql.Date;
import java.time.LocalDate;

public record GreetingRecord(String message, LocalDate date, Date sqlDate) {

}
