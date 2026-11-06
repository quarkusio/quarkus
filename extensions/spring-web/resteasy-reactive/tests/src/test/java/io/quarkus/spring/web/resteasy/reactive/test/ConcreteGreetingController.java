package io.quarkus.spring.web.resteasy.reactive.test;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/concrete-greeting")
public class ConcreteGreetingController {

    @GetMapping
    public String hello() {
        return "hello from concrete";
    }
}
