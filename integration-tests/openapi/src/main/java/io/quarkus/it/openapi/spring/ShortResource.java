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
public class ShortResource {

    @GetMapping("/justShort")
    public Short justShort() {
        return (short) 0;
    }

    @PostMapping("/justShort")
    public Short justShort(Short body) {
        return body;
    }

    @GetMapping("/justPrimitiveShort")
    public short justPrimitiveShort() {
        return (short) 0;
    }

    @PostMapping("/justPrimitiveShort")
    public short justPrimitiveShort(short body) {
        return body;
    }

    @GetMapping("/responseEntityShort")
    public ResponseEntity<Short> responseEntityShort() {
        return ResponseEntity.ok((short) 0);
    }

    @PostMapping("/responseEntityShort")
    public ResponseEntity<Short> responseEntityShort(Short body) {
        return ResponseEntity.ok(body);
    }

    @GetMapping("/optionalShort")
    public Optional<Short> optionalShort() {
        return Optional.of((short) 0);
    }

    @PostMapping("/optionalShort")
    public Optional<Short> optionalShort(Optional<Short> body) {
        return body;
    }

    @GetMapping("/uniShort")
    public Uni<Short> uniShort() {
        return Uni.createFrom().item((short) 0);
    }

    @GetMapping("/completionStageShort")
    public CompletionStage<Short> completionStageShort() {
        return CompletableFuture.completedStage((short) 0);
    }

    @GetMapping("/completedFutureShort")
    public CompletableFuture<Short> completedFutureShort() {
        return CompletableFuture.completedFuture((short) 0);
    }

    @GetMapping("/listShort")
    public List<Short> listShort() {
        return Arrays.asList(new Short[] { (short) 0 });
    }

    @PostMapping("/listShort")
    public List<Short> listShort(List<Short> body) {
        return body;
    }

    @GetMapping("/arrayShort")
    public Short[] arrayShort() {
        return new Short[] { (short) 0 };
    }

    @PostMapping("/arrayShort")
    public Short[] arrayShort(Short[] body) {
        return body;
    }

    @GetMapping("/arrayPrimitiveShort")
    public short[] arrayPrimitiveShort() {
        return new short[] { (short) 0 };
    }

    @PostMapping("/arrayPrimitiveShort")
    public short[] arrayPrimitiveShort(short[] body) {
        return body;
    }

    @GetMapping("/mapShort")
    public Map<Short, Short> mapShort() {
        Map<Short, Short> m = new HashMap<>();
        m.put((short) 0, (short) 0);
        return m;
    }

    @PostMapping("/mapShort")
    public Map<Short, Short> mapShort(Map<Short, Short> body) {
        return body;
    }
}
