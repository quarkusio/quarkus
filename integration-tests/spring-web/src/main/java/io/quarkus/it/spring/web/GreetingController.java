package io.quarkus.it.spring.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/greeting")
public class GreetingController {

    final GreetingService greetingService;

    public GreetingController(GreetingService greetingService) {
        this.greetingService = greetingService;
    }

    @GetMapping(path = "/json/{message}")
    public Greeting greet(@PathVariable(name = "message") String message) {
        return greetingService.greet(message);
    }

    @PostMapping(path = "/person")
    public Greeting newGreeting(@RequestBody Person person) {
        return new Greeting("hello " + person.getName());
    }
}
