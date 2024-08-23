package io.quarkus.it.openapi.spring;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
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
public class FileResource {
    @GetMapping("/justFile/{fileName}")
    public File justFile(@PathVariable("fileName") String filename) {
        return toFile(filename);
    }

    @PostMapping("/justFile")
    public File justFile(File file) {
        return file;
    }

    @GetMapping("/responseEntityFile/{fileName}")
    public ResponseEntity<File> restResponseFile(@PathVariable("fileName") String filename) {
        return ResponseEntity.ok(toFile(filename));
    }

    @PostMapping("/responseEntityFile")
    public ResponseEntity<File> restResponseFile(File file) {
        return ResponseEntity.ok(file);
    }

    @GetMapping("/optionalFile/{fileName}")
    public Optional<File> optionalFile(@PathVariable("fileName") String filename) {
        return Optional.of(toFile(filename));
    }

    @PostMapping("/optionalFile")
    public Optional<File> optionalFile(Optional<File> file) {
        return file;
    }

    @GetMapping("/uniFile/{fileName}")
    public Uni<File> uniFile(@PathVariable("fileName") String filename) {
        return Uni.createFrom().item(toFile(filename));
    }

    @GetMapping("/completionStageFile/{fileName}")
    public CompletionStage<File> completionStageFile(@PathVariable("fileName") String filename) {
        return CompletableFuture.completedStage(toFile(filename));
    }

    @GetMapping("/completedFutureFile/{fileName}")
    public CompletableFuture<File> completedFutureFile(@PathVariable("fileName") String filename) {
        return CompletableFuture.completedFuture(toFile(filename));
    }

    private File toFile(String filename) {
        try {
            String f = URLDecoder.decode(filename, "UTF-8");
            return new File(f);
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }
}
