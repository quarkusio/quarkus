package io.quarkus.rest.data.panache.runtime.hal;

import java.time.LocalDate;
import java.time.Month;

public class PublishedBook extends Book {

    public LocalDate publicationDate = LocalDate.of(2021, Month.AUGUST, 31);

    public PublishedBook(long id, String name) {
        super(id, name);
    }
}
