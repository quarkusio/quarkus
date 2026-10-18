package io.quarkus.it.spring.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloControllerWithoutRequestMapping {

    @GetMapping("/hello")
    public String hello() {
        return "hello";
    }
}
