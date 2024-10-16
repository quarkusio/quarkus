package io.quarkus.it.openapi.spring;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
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
public class BigDecimalResource {
    @GetMapping("/justBigDecimal")
    public BigDecimal justBigDecimal() {
        return new BigDecimal("0");
    }

    @PostMapping("/justBigDecimal")
    public BigDecimal justBigDecimal(BigDecimal body) {
        return body;
    }

    @GetMapping("/responseEntityBigDecimal")
    public ResponseEntity<BigDecimal> responseEntityBigDecimal() {
        return ResponseEntity.ok(new BigDecimal("0"));
    }

    @PostMapping("/responseEntityBigDecimal")
    public ResponseEntity<BigDecimal> responseEntityBigDecimal(BigDecimal body) {
        return ResponseEntity.ok(body);
    }

    @GetMapping("/optionalBigDecimal")
    public Optional<BigDecimal> optionalBigDecimal() {
        return Optional.of(new BigDecimal("0"));
    }

    @PostMapping("/optionalBigDecimal")
    public Optional<BigDecimal> optionalBigDecimal(Optional<BigDecimal> body) {
        return body;
    }

    @GetMapping("/uniBigDecimal")
    public Uni<BigDecimal> uniBigDecimal() {
        return Uni.createFrom().item(new BigDecimal("0"));
    }

    @GetMapping("/completionStageBigDecimal")
    public CompletionStage<BigDecimal> completionStageBigDecimal() {
        return CompletableFuture.completedStage(new BigDecimal("0"));
    }

    @GetMapping("/completedFutureBigDecimal")
    public CompletableFuture<BigDecimal> completedFutureBigDecimal() {
        return CompletableFuture.completedFuture(new BigDecimal("0"));
    }

    @GetMapping("/listBigDecimal")
    public List<BigDecimal> listBigDecimal() {
        return Arrays.asList(new BigDecimal[] { new BigDecimal("0") });
    }

    @PostMapping("/listBigDecimal")
    public List<BigDecimal> listBigDecimal(List<BigDecimal> body) {
        return body;
    }

    @GetMapping("/arrayBigDecimal")
    public BigDecimal[] arrayBigDecimal() {
        return new BigDecimal[] { new BigDecimal("0") };
    }

    @PostMapping("/arrayBigDecimal")
    public BigDecimal[] arrayBigDecimal(BigDecimal[] body) {
        return body;
    }

}
