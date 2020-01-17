package io.quarkus.it.panache.rest.common;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.annotation.JsonFormat;

public class AuthorDto {

    public final Long id;

    public final String name;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    public final LocalDate dob;

    public AuthorDto(Long id, String name, LocalDate dob) {
        this.id = id;
        this.name = name;
        this.dob = dob;
    }

    public String dobAsString() {
        return dob.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
}
