package io.quarkus.it.openapi.spring;

import java.net.MalformedURLException;
import java.net.URL;
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
public class URLResource {

    @GetMapping("/justURL")
    public URL justURL() {
        return url();
    }

    @PostMapping("/justURL")
    public URL justURL(URL url) {
        return url;
    }

    @GetMapping("/restResponseURL")
    public ResponseEntity<URL> restResponseURL() {
        return ResponseEntity.ok(url());
    }

    @PostMapping("/restResponseURL")
    public ResponseEntity<URL> restResponseURL(URL body) {
        return ResponseEntity.ok(body);
    }

    @GetMapping("/optionalURL")
    public Optional<URL> optionalURL() {
        return Optional.of(url());
    }

    @PostMapping("/optionalURL")
    public Optional<URL> optionalURL(Optional<URL> body) {
        return body;
    }

    @GetMapping("/uniURL")
    public Uni<URL> uniURL() {
        return Uni.createFrom().item(url());
    }

    @GetMapping("/completionStageURL")
    public CompletionStage<URL> completionStageURL() {
        return CompletableFuture.completedStage(url());
    }

    @GetMapping("/completedFutureURL")
    public CompletableFuture<URL> completedFutureURL() {
        return CompletableFuture.completedFuture(url());
    }

    @GetMapping("/listURL")
    public List<URL> listURL() {
        return Arrays.asList(new URL[] { url() });
    }

    @PostMapping("/listURL")
    public List<URL> listURL(List<URL> body) {
        return body;
    }

    @GetMapping("/arrayURL")
    public URL[] arrayURL() {
        return new URL[] { url() };
    }

    @PostMapping("/arrayURL")
    public URL[] arrayURL(URL[] body) {
        return body;
    }

    @GetMapping("/mapURL")
    public Map<String, URL> mapURL() {
        Map<String, URL> m = new HashMap<>();
        m.put("mapURL", url());
        return m;
    }

    @PostMapping("/mapURL")
    public Map<String, URL> mapURL(Map<String, URL> body) {
        return body;
    }

    private URL url() {
        try {
            return new URL("https://quarkus.io/");
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }
}
