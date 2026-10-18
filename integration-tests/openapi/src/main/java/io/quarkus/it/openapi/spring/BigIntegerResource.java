package io.quarkus.it.openapi.spring;

import java.math.BigInteger;
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
public class BigIntegerResource {
    @GetMapping("/justBigInteger")
    public BigInteger justBigInteger() {
        return new BigInteger("0");
    }

    @PostMapping("/justBigInteger")
    public BigInteger justBigInteger(BigInteger body) {
        return body;
    }

    @GetMapping("/responseEntityBigInteger")
    public ResponseEntity<BigInteger> responseEntityBigInteger() {
        return ResponseEntity.ok(new BigInteger("0"));
    }

    @PostMapping("/responseEntityBigInteger")
    public ResponseEntity<BigInteger> responseEntityBigInteger(BigInteger body) {
        return ResponseEntity.ok(body);
    }

    @GetMapping("/optionalBigInteger")
    public Optional<BigInteger> optionalBigInteger() {
        return Optional.of(new BigInteger("0"));
    }

    @PostMapping("/optionalBigInteger")
    public Optional<BigInteger> optionalBigInteger(Optional<BigInteger> body) {
        return body;
    }

    @GetMapping("/uniBigInteger")
    public Uni<BigInteger> uniBigInteger() {
        return Uni.createFrom().item(new BigInteger("0"));
    }

    @GetMapping("/completionStageBigInteger")
    public CompletionStage<BigInteger> completionStageBigInteger() {
        return CompletableFuture.completedStage(new BigInteger("0"));
    }

    @GetMapping("/completedFutureBigInteger")
    public CompletableFuture<BigInteger> completedFutureBigInteger() {
        return CompletableFuture.completedFuture(new BigInteger("0"));
    }

    @GetMapping("/listBigInteger")
    public List<BigInteger> listBigInteger() {
        return Arrays.asList(new BigInteger[] { new BigInteger("0") });
    }

    @PostMapping("/listBigInteger")
    public List<BigInteger> listBigInteger(List<BigInteger> body) {
        return body;
    }

    @GetMapping("/arrayBigInteger")
    public BigInteger[] arrayBigInteger() {
        return new BigInteger[] { new BigInteger("0") };
    }

    @PostMapping("/arrayBigInteger")
    public BigInteger[] arrayBigInteger(BigInteger[] body) {
        return body;
    }

}
