package io.quarkus.it.spring.web;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/greeting")
public class GreetingController {

    final GreetingService greetingService;

    public GreetingController(GreetingService greetingService) {
        this.greetingService = greetingService;
    }

    @GetMapping(path = "/json/{message}")
    public Greeting greet(@PathVariable String message, @RequestParam String suffix) {
        return greetingService.greet(message + suffix);
    }

    @GetMapping(path = "/re/json/{message}")
    public ResponseEntity<Greeting> responseEntityGreeting(@PathVariable String message, @RequestParam String suffix) {
        return ResponseEntity.ok(greetingService.greet(message + suffix));
    }

    @PostMapping(path = "/person")
    public Greeting newGreeting(@RequestBody @Valid Person person) {
        return new Greeting("hello " + person.getName());
    }
}
