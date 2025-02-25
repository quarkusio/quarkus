package io.quarkus.spring.web.resteasy.reactive.test;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/" + ResponseEntityController.CONTROLLER_PATH)
public class ResponseEntityController {

    public static final String CONTROLLER_PATH = "re";

    @GetMapping("/noContent")
    public ResponseEntity<String> noContent() {
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/string", produces = "text/plain")
    public ResponseEntity<String> string() {
        return ResponseEntity.ok("hello world");
    }

    @GetMapping(value = "/json")
    public ResponseEntity<SomeClass> jsonPlusHeaders() {
        return ResponseEntity.ok().header("custom-header", "somevalue").body(new SomeClass("dummy"));
    }

    @GetMapping(value = "/json2", produces = "application/json")
    public ResponseEntity<?> responseEntityWithoutType() {
        return ResponseEntity.ok().body(new SomeClass("dummy"));
    }

    @GetMapping(value = "/custom-json")
    public ResponseEntity<SomeClass> customJson() {
        return ResponseEntity.ok().header("custom-header", "somevalue").header("content-type", "application/jsontest")
                .body(new SomeClass("dummy"));
    }

    @GetMapping(value = "/content-type")
    public ResponseEntity<SomeClass> contentType() {
        return ResponseEntity.ok().header("custom-header", "somevalue")
                .contentType(org.springframework.http.MediaType.valueOf("application/jsontest"))
                .body(new SomeClass("dummy"));
    }
}
