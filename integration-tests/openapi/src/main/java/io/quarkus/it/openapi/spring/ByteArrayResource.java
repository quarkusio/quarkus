package io.quarkus.it.openapi.spring;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.smallrye.mutiny.Uni;

@RestController
@RequestMapping(value = "/spring/defaultContentType")
public class ByteArrayResource {
    @GetMapping("/justByteArray/{fileName}")
    public byte[] justByteArray(@PathVariable("fileName") String filename) {
        return toByteArray(filename);
    }

    @PostMapping("/justByteArray")
    public byte[] justByteArray(byte[] inputStream) {
        return inputStream;
    }

    @GetMapping("/responseEntityByteArray/{fileName}")
    public ResponseEntity<byte[]> responseEntityByteArray(@PathVariable("fileName") String filename) {
        return ResponseEntity.ok(toByteArray(filename));
    }

    @PostMapping("/responseEntityByteArray")
    public ResponseEntity<byte[]> responseEntityByteArray(byte[] inputStream) {
        return ResponseEntity.ok(inputStream);
    }

    @GetMapping("/optionalByteArray/{fileName}")
    public Optional<byte[]> optionalByteArray(@PathVariable("fileName") String filename) {
        return Optional.of(toByteArray(filename));
    }

    @PostMapping("/optionalByteArray")
    public Optional<byte[]> optionalByteArray(Optional<byte[]> inputStream) {
        return inputStream;
    }

    @GetMapping("/uniByteArray/{fileName}")
    public Uni<byte[]> uniByteArray(@PathVariable("fileName") String filename) {
        return Uni.createFrom().item(toByteArray(filename));
    }

    @GetMapping("/completionStageByteArray/{fileName}")
    public CompletionStage<byte[]> completionStageByteArray(@PathVariable("fileName") String filename) {
        return CompletableFuture.completedStage(toByteArray(filename));
    }

    @GetMapping("/completedFutureByteArray/{fileName}")
    public CompletableFuture<byte[]> completedFutureByteArray(@PathVariable("fileName") String filename) {
        return CompletableFuture.completedFuture(toByteArray(filename));
    }

    private byte[] toByteArray(String filename) {
        try {
            String f = URLDecoder.decode(filename, "UTF-8");
            return Files.readAllBytes(Paths.get(f));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
