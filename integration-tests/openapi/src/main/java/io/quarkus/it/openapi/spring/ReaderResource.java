package io.quarkus.it.openapi.spring;

import java.io.IOException;
import java.io.Reader;
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
public class ReaderResource {
    @GetMapping("/justReader/{fileName}")
    public Reader justReader(@PathVariable("fileName") String filename) {
        return toReader(filename);
    }

    @PostMapping("/justReader")
    public Reader justReader(Reader file) {
        return file;
    }

    @GetMapping("/responseEntityReader/{fileName}")
    public ResponseEntity<Reader> restResponseReader(@PathVariable("fileName") String filename) {
        return ResponseEntity.ok(toReader(filename));
    }

    @PostMapping("/responseEntityReader")
    public ResponseEntity<Reader> restResponseReader(Reader file) {
        return ResponseEntity.ok(file);
    }

    @GetMapping("/optionalReader/{fileName}")
    public Optional<Reader> optionalReader(@PathVariable("fileName") String filename) {
        return Optional.of(toReader(filename));
    }

    @PostMapping("/optionalReader")
    public Optional<Reader> optionalReader(Optional<Reader> file) {
        return file;
    }

    @GetMapping("/uniReader/{fileName}")
    public Uni<Reader> uniReader(@PathVariable("fileName") String filename) {
        return Uni.createFrom().item(toReader(filename));
    }

    @GetMapping("/completionStageReader/{fileName}")
    public CompletionStage<Reader> completionStageReader(@PathVariable("fileName") String filename) {
        return CompletableFuture.completedStage(toReader(filename));
    }

    @GetMapping("/completedFutureReader/{fileName}")
    public CompletableFuture<Reader> completedFutureReader(@PathVariable("fileName") String filename) {
        return CompletableFuture.completedFuture(toReader(filename));
    }

    private Reader toReader(String filename) {
        try {
            String f = URLDecoder.decode(filename, "UTF-8");
            return Files.newBufferedReader(Paths.get(f));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
