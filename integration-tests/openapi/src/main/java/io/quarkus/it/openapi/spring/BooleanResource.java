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

import io.smallrye.mutiny.Uni;

@RestController
@RequestMapping(value = "/spring/defaultContentType")
public class BooleanResource {

    @GetMapping("/justBoolean")
    public Boolean justBoolean() {
        return Boolean.TRUE;
    }

    @PostMapping("/justBoolean")
    public Boolean justBoolean(Boolean body) {
        return body;
    }

    @GetMapping("/justBool")
    public boolean justBool() {
        return true;
    }

    @PostMapping("/justBool")
    public boolean justBool(boolean body) {
        return body;
    }

    @GetMapping("/responseEntityBoolean")
    public ResponseEntity<Boolean> responseEntityBoolean() {
        return ResponseEntity.ok(Boolean.TRUE);
    }

    @PostMapping("/responseEntityBoolean")
    public ResponseEntity<Boolean> responseEntityBoolean(Boolean body) {
        return ResponseEntity.ok(body);
    }

    @GetMapping("/optionalBoolean")
    public Optional<Boolean> optionalBoolean() {
        return Optional.of(Boolean.TRUE);
    }

    @PostMapping("/optionalBoolean")
    public Optional<Boolean> optionalBoolean(Optional<Boolean> body) {
        return body;
    }

    @GetMapping("/uniBoolean")
    public Uni<Boolean> uniBoolean() {
        return Uni.createFrom().item(Boolean.TRUE);
    }

    @GetMapping("/completionStageBoolean")
    public CompletionStage<Boolean> completionStageBoolean() {
        return CompletableFuture.completedStage(Boolean.TRUE);
    }

    @GetMapping("/completedFutureBoolean")
    public CompletableFuture<Boolean> completedFutureBoolean() {
        return CompletableFuture.completedFuture(Boolean.TRUE);
    }

    @GetMapping("/listBoolean")
    public List<Boolean> listBoolean() {
        return Arrays.asList(new Boolean[] { Boolean.TRUE });
    }

    @PostMapping("/listBoolean")
    public List<Boolean> listBoolean(List<Boolean> body) {
        return body;
    }

    @GetMapping("/arrayBoolean")
    public Boolean[] arrayBoolean() {
        return new Boolean[] { Boolean.TRUE };
    }

    @PostMapping("/arrayBoolean")
    public Boolean[] arrayBoolean(Boolean[] body) {
        return body;
    }

    @GetMapping("/arrayBool")
    public boolean[] arrayBool() {
        return new boolean[] { true };
    }

    @PostMapping("/arrayBool")
    public boolean[] arrayBool(boolean[] body) {
        return body;
    }

    @GetMapping("/mapBoolean")
    public Map<Boolean, Boolean> mapBoolean() {
        Map<Boolean, Boolean> m = new HashMap<>();
        m.put(true, true);
        return m;
    }

    @PostMapping("/mapBoolean")
    public Map<Boolean, Boolean> mapBoolean(Map<Boolean, Boolean> body) {
        return body;
    }
}
