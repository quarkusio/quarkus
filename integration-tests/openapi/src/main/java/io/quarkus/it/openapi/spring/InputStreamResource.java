package io.quarkus.it.openapi.spring;

import java.io.IOException;
import java.io.InputStream;
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
public class InputStreamResource {
    @GetMapping("/justInputStream/{fileName}")
    public InputStream justInputStream(@PathVariable("fileName") String filename) {
        return toInputStream(filename);
    }

    @PostMapping("/justInputStream")
    public InputStream justInputStream(InputStream file) {
        return file;
    }

    @GetMapping("/responseEntityInputStream/{fileName}")
    public ResponseEntity<InputStream> restResponseInputStream(@PathVariable("fileName") String filename) {
        return ResponseEntity.ok(toInputStream(filename));
    }

    @PostMapping("/responseEntityInputStream")
    public ResponseEntity<InputStream> restResponseInputStream(InputStream file) {
        return ResponseEntity.ok(file);
    }

    @GetMapping("/optionalInputStream/{fileName}")
    public Optional<InputStream> optionalInputStream(@PathVariable("fileName") String filename) {
        return Optional.of(toInputStream(filename));
    }

    @PostMapping("/optionalInputStream")
    public Optional<InputStream> optionalInputStream(Optional<InputStream> file) {
        return file;
    }

    @GetMapping("/uniInputStream/{fileName}")
    public Uni<InputStream> uniInputStream(@PathVariable("fileName") String filename) {
        return Uni.createFrom().item(toInputStream(filename));
    }

    @GetMapping("/completionStageInputStream/{fileName}")
    public CompletionStage<InputStream> completionStageInputStream(@PathVariable("fileName") String filename) {
        return CompletableFuture.completedStage(toInputStream(filename));
    }

    @GetMapping("/completedFutureInputStream/{fileName}")
    public CompletableFuture<InputStream> completedFutureInputStream(@PathVariable("fileName") String filename) {
        return CompletableFuture.completedFuture(toInputStream(filename));
    }

    private InputStream toInputStream(String filename) {
        try {
            String f = URLDecoder.decode(filename, "UTF-8");
            return Files.newInputStream(Paths.get(f));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
