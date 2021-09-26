package io.quarkus.it.spring.web;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BookController {

    @GetMapping(produces = MediaType.APPLICATION_XML_VALUE, path = "/book")
    public Book someBook() {
        return new Book("Guns germs and steel");
    }
}
