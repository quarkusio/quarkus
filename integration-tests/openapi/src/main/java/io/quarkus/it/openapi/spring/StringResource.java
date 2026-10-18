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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.smallrye.mutiny.Uni;

@RestController
@RequestMapping(value = "/spring/defaultContentType")
public class StringResource {

    @GetMapping("/justString")
    public String justString() {
        return "justString";
    }

    @PostMapping("/justString")
    public String justString(@RequestBody String body) {
        return body;
    }

    @GetMapping("/responseEntityString")
    public ResponseEntity<String> responseEntityString() {
        return ResponseEntity.ok("responseEntityString");
    }

    @PostMapping("/responseEntityString")
    public ResponseEntity<String> responseEntityString(@RequestBody String body) {
        return ResponseEntity.ok(body);
    }

    @GetMapping("/optionalString")
    public Optional<String> optionalString() {
        return Optional.of("optionalString");
    }

    @PostMapping("/optionalString")
    public Optional<String> optionalString(@RequestBody Optional<String> body) {
        return body;
    }

    @GetMapping("/uniString")
    public Uni<String> uniString() {
        return Uni.createFrom().item("uniString");
    }

    @GetMapping("/completionStageString")
    public CompletionStage<String> completionStageString() {
        return CompletableFuture.completedStage("completionStageString");
    }

    @GetMapping("/completedFutureString")
    public CompletableFuture<String> completedFutureString() {
        return CompletableFuture.completedFuture("completedFutureString");
    }

    @GetMapping("/listString")
    public List<String> listString() {
        return Arrays.asList(new String[] { "listString" });
    }

    @PostMapping("/listString")
    public List<String> listString(@RequestBody List<String> body) {
        return body;
    }

    @GetMapping("/arrayString")
    public String[] arrayString() {
        return new String[] { "arrayString" };
    }

    @PostMapping("/arrayString")
    public String[] arrayString(@RequestBody String[] body) {
        return body;
    }

    @GetMapping("/mapString")
    public Map<String, String> mapString() {
        Map<String, String> m = new HashMap<>();
        m.put("mapString", "mapString");
        return m;
    }

    @PostMapping("/mapString")
    public Map<String, String> mapString(Map<String, String> body) {
        return body;
    }
}
