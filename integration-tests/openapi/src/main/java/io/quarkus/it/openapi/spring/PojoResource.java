package io.quarkus.it.openapi.spring;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.quarkus.it.openapi.Greeting;
import io.smallrye.mutiny.Uni;

@RestController
@RequestMapping(value = "/spring/defaultContentType")
public class PojoResource {

    @GetMapping("/justPojo")
    public Greeting justPojo() {
        return new Greeting(0, "justPojo");
    }

    @PostMapping("/justPojo")
    public Greeting justPojo(Greeting greeting) {
        return greeting;
    }

    @GetMapping("/responseEntityPojo")
    public ResponseEntity<Greeting> responseEntityPojo() {
        return ResponseEntity.ok(new Greeting(0, "responseEntityPojo"));
    }

    @PostMapping("/responseEntityPojo")
    public ResponseEntity<Greeting> responseEntityPojo(Greeting greeting) {
        return ResponseEntity.ok(greeting);
    }

    @GetMapping("/optionalPojo")
    public Optional<Greeting> optionalPojo() {
        return Optional.of(new Greeting(0, "optionalPojo"));
    }

    @PostMapping("/optionalPojo")
    public Optional<Greeting> optionalPojo(Optional<Greeting> greeting) {
        return greeting;
    }

    @GetMapping("/uniPojo")
    public Uni<Greeting> uniPojo() {
        return Uni.createFrom().item(new Greeting(0, "uniPojo"));
    }

    @GetMapping("/completionStagePojo")
    public CompletionStage<Greeting> completionStagePojo() {
        return CompletableFuture.completedStage(new Greeting(0, "completionStagePojo"));
    }

    @GetMapping("/completedFuturePojo")
    public CompletableFuture<Greeting> completedFuturePojo() {
        return CompletableFuture.completedFuture(new Greeting(0, "completedFuturePojo"));
    }

    @GetMapping("/listPojo")
    public List<Greeting> listPojo() {
        return Arrays.asList(new Greeting[] { new Greeting(0, "listPojo") });
    }

    @PostMapping("/listPojo")
    public List<Greeting> listPojo(List<Greeting> greeting) {
        return greeting;
    }

    @GetMapping("/arrayPojo")
    public Greeting[] arrayPojo() {
        return new Greeting[] { new Greeting(0, "arrayPojo") };
    }

    @PostMapping("/arrayPojo")
    public Greeting[] arrayPojo(Greeting[] greeting) {
        return greeting;
    }

    @GetMapping("/mapPojo")
    public Map<String, Greeting> mapPojo() {
        Map<String, Greeting> m = new HashMap<>();
        Greeting g = new Greeting(0, "mapPojo");
        m.put("mapPojo", g);
        return m;
    }

    @PostMapping("/mapPojo")
    public Map<String, Greeting> mapPojo(Map<String, Greeting> body) {
        return body;
    }
}
