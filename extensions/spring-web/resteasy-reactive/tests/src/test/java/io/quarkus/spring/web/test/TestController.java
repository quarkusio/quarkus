package io.quarkus.spring.web.test;

import javax.ws.rs.core.MediaType;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/" + TestController.CONTROLLER_PATH)
public class TestController {

    public static final String CONTROLLER_PATH = "spring";

    @GetMapping("/hello")
    public String string(@RequestParam String name) {
        return "hello " + name;
    }

    @RequestMapping("/hello4")
    public String requestMappingNoMethod(@RequestParam String name) {
        return "hello " + name;
    }

    @GetMapping("yolo")
    public String yolo() {
        return "yolo";
    }

    @GetMapping("/hello2")
    public String stringWithDefaultParamValue(@RequestParam(name = "name", defaultValue = "world") String name) {
        return "hello " + name;
    }

    @GetMapping("/hello3")
    public String stringWithNameValue(@RequestParam(name = "name") String name) {
        return "hello " + name;
    }

    @GetMapping("/wildcard/*/{name}")
    public String pathWithWildcard(@PathVariable("name") String name) {
        return name;
    }

    @RequestMapping(value = "/wildcard2/*/{name}/*", method = RequestMethod.GET)
    public String pathWithMultipleWildcards(@PathVariable("name") String name) {
        return name;
    }

    @GetMapping("/antwildcard/**")
    public String pathWithAntStyleWildcard() {
        return "ant";
    }

    @GetMapping("/ca?s")
    public String pathWithCharacterWildCard() {
        return "single";
    }

    @GetMapping("/car?/s?o?/info")
    public String pathWithMultipleCharacterWildCards() {
        return "multiple";
    }

    @GetMapping("/int/{num}")
    public Integer intPathVariable(@PathVariable("num") Integer number) {
        return number + 1;
    }

    @GetMapping("/{msg}")
    public String stringPathVariable(@PathVariable("msg") String message) {
        return message;
    }

    @GetMapping(path = "/json/{message}")
    public SomeClass json(@PathVariable String message) {
        return new SomeClass(message);
    }

    @RequestMapping(method = RequestMethod.GET, path = "/json2/{message}", produces = MediaType.APPLICATION_JSON)
    public SomeClass jsonFromRequestMapping(@PathVariable String message) {
        return new SomeClass(message);
    }

    @PostMapping(path = "/json", produces = MediaType.TEXT_PLAIN, consumes = MediaType.APPLICATION_JSON)
    public String postWithJsonBody(@RequestBody SomeClass someClass) {
        return someClass.getMessage();
    }

    @RequestMapping(path = "/json2", method = RequestMethod.POST, produces = MediaType.TEXT_PLAIN, consumes = MediaType.APPLICATION_JSON)
    public String postWithJsonBodyFromRequestMapping(@RequestBody SomeClass someClass) {
        return someClass.getMessage();
    }

    @PutMapping(path = "/json3")
    public Greeting multipleInputAndJsonResponse(@RequestBody SomeClass someClass,
            @RequestParam(value = "suffix") String suffix) {
        return new Greeting(someClass.getMessage() + suffix);
    }

    public void doNothing() {

    }
}
