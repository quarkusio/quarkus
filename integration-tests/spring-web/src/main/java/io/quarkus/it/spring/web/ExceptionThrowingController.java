package io.quarkus.it.spring.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/exception")
public class ExceptionThrowingController {

    @RequestMapping(method = RequestMethod.GET, path = "/first")
    public void first() throws FirstException {
        throw new FirstException("first");
    }

    @GetMapping(path = "/second")
    public String second() {
        throw new SecondException();
    }

    @GetMapping("/void")
    public void runtimeException() {
        throw new RuntimeException();
    }

    @GetMapping("/unannotated")
    public void unannotated() {
        throw new UnannotatedException();
    }

    @GetMapping("/responseEntity")
    public Greeting handledByResponseEntity() {
        throw new IllegalStateException("bad state");
    }

    @GetMapping("/responseEntityFromVoidReturningMethod")
    public void handledByResponseEntityFromVoidReturningMethod() {
        throw new IllegalStateException("bad state");
    }

    @GetMapping("/pojo")
    public Greeting greetingWithIllegalArgumentException() {
        throw new IllegalArgumentException("hello from error");
    }

    @GetMapping("/pojoWithVoidReturnType")
    public void greetingWithIllegalArgumentExceptionAndVoidReturnType() {
        throw new IllegalArgumentException("hello from error");
    }

    @GetMapping("/re")
    public ResponseEntity<Greeting> responseEntityWithIllegalArgumentException() {
        throw new IllegalStateException("hello from error");
    }

}
