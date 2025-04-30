package io.quarkus.it.openapi.spring;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
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
public class IntegerResource {

    @GetMapping("/justInteger")
    public Integer justInteger() {
        return 0;
    }

    @PostMapping("/justInteger")
    public Integer justInteger(Integer body) {
        return body;
    }

    @GetMapping("/justInt")
    public int justInt() {
        return 0;
    }

    @PostMapping("/justInt")
    public int justInt(int body) {
        return body;
    }

    @GetMapping("/responseEntityInteger")
    public ResponseEntity<Integer> responseEntityInteger() {
        return ResponseEntity.ok(0);
    }

    @PostMapping("/responseEntityInteger")
    public ResponseEntity<Integer> responseEntityInteger(Integer body) {
        return ResponseEntity.ok(body);
    }

    @GetMapping("/optionalInteger")
    public Optional<Integer> optionalInteger() {
        return Optional.of(0);
    }

    @PostMapping("/optionalInteger")
    public Optional<Integer> optionalInteger(Optional<Integer> body) {
        return body;
    }

    @GetMapping("/optionalInt")
    public OptionalInt optionalInt() {
        return OptionalInt.of(0);
    }

    @PostMapping("/optionalInt")
    public OptionalInt optionalInt(OptionalInt body) {
        return body;
    }

    @GetMapping("/uniInteger")
    public Uni<Integer> uniInteger() {
        return Uni.createFrom().item(0);
    }

    @GetMapping("/completionStageInteger")
    public CompletionStage<Integer> completionStageInteger() {
        return CompletableFuture.completedStage(0);
    }

    @GetMapping("/completedFutureInteger")
    public CompletableFuture<Integer> completedFutureInteger() {
        return CompletableFuture.completedFuture(0);
    }

    @GetMapping("/listInteger")
    public List<Integer> listInteger() {
        return Arrays.asList(new Integer[] { 0 });
    }

    @PostMapping("/listInteger")
    public List<Integer> listInteger(List<Integer> body) {
        return body;
    }

    @GetMapping("/arrayInteger")
    public Integer[] arrayInteger() {
        return new Integer[] { 0 };
    }

    @PostMapping("/arrayInteger")
    public Integer[] arrayInteger(Integer[] body) {
        return body;
    }

    @GetMapping("/arrayInt")
    public int[] arrayInt() {
        return new int[] { 0 };
    }

    @PostMapping("/arrayInt")
    public int[] arrayInt(int[] body) {
        return body;
    }

    @GetMapping("/mapInteger")
    public Map<Integer, Integer> mapInteger() {
        Map<Integer, Integer> m = new HashMap<>();
        m.put(0, 0);
        return m;
    }

    @PostMapping("/mapInteger")
    public Map<Integer, Integer> mapInteger(Map<Integer, Integer> body) {
        return body;
    }
}
