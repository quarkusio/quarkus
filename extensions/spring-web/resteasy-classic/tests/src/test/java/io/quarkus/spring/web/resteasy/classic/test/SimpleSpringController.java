package io.quarkus.spring.web.resteasy.classic.test;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/simple")
public class SimpleSpringController {

    @GetMapping
    public String hello() {
        return "hello";
    }
}
