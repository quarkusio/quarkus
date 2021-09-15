package io.quarkus.spring.web.test.basic;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GreetingControllerWithNoRequestMapping {

    @GetMapping("/hello")
    public String hello() {
        return "hello world";
    }
}
