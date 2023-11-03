package io.quarkus.it.openapi.spring;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
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
public class LongResource {

    @GetMapping("/justLong")
    public Long justLong() {
        return 0L;
    }

    @PostMapping("/justLong")
    public Long justLong(Long body) {
        return body;
    }

    @GetMapping("/justPrimitiveLong")
    public long justPrimitiveLong() {
        return 0L;
    }

    @PostMapping("/justPrimitiveLong")
    public long justPrimitiveLong(long body) {
        return body;
    }

    @GetMapping("/responseEntityLong")
    public ResponseEntity<Long> responseEntityLong() {
        return ResponseEntity.ok(0L);
    }

    @PostMapping("/responseEntityLong")
    public ResponseEntity<Long> responseEntityLong(Long body) {
        return ResponseEntity.ok(body);
    }

    @GetMapping("/optionalLong")
    public Optional<Long> optionalLong() {
        return Optional.of(0L);
    }

    @PostMapping("/optionalLong")
    public Optional<Long> optionalLong(Optional<Long> body) {
        return body;
    }

    @GetMapping("/optionalPrimitiveLong")
    public OptionalLong optionalPrimitiveLong() {
        return OptionalLong.of(0L);
    }

    @PostMapping("/optionalPrimitiveLong")
    public OptionalLong optionalPrimitiveLong(OptionalLong body) {
        return body;
    }

    @GetMapping("/uniLong")
    public Uni<Long> uniLong() {
        return Uni.createFrom().item(0L);
    }

    @GetMapping("/completionStageLong")
    public CompletionStage<Long> completionStageLong() {
        return CompletableFuture.completedStage(0L);
    }

    @GetMapping("/completedFutureLong")
    public CompletableFuture<Long> completedFutureLong() {
        return CompletableFuture.completedFuture(0L);
    }

    @GetMapping("/listLong")
    public List<Long> listLong() {
        return Arrays.asList(new Long[] { 0L });
    }

    @PostMapping("/listLong")
    public List<Long> listLong(List<Long> body) {
        return body;
    }

    @GetMapping("/arrayLong")
    public Long[] arrayLong() {
        return new Long[] { 0L };
    }

    @PostMapping("/arrayLong")
    public Long[] arrayLong(Long[] body) {
        return body;
    }

    @GetMapping("/arrayPrimitiveLong")
    public long[] arrayPrimitiveLong() {
        return new long[] { 0L };
    }

    @PostMapping("/arrayPrimitiveLong")
    public long[] arrayPrimitiveLong(long[] body) {
        return body;
    }

    @GetMapping("/mapLong")
    public Map<Long, Long> mapLong() {
        Map<Long, Long> m = new HashMap<>();
        m.put(0L, 0L);
        return m;
    }

    @PostMapping("/mapLong")
    public Map<Long, Long> mapLong(Map<Long, Long> body) {
        return body;
    }
}
