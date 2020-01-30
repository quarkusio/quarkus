package io.quarkus.it.spring.web;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/exception")
public class ExceptionThrowingController {

    @RequestMapping(method = RequestMethod.GET, path = "/unhandled/exception")
    public void voidWithUnhandledAnnotatedException() throws UnhandledAnnotatedException {
        throw new UnhandledAnnotatedException("unhandled");
    }

    @GetMapping(path = "/unhandled/runtime")
    public String stringWithUnhandledAnnotatedRuntimeException() {
        throw new UnhandledAnnotatedRuntimeException();
    }

    @GetMapping("/runtime")
    public void voidWithRuntimeException() {
        throw new RuntimeException();
    }

    @GetMapping("/unannotated")
    public void voidWithUnannotatedException() {
        throw new HandledUnannotatedException();
    }

    @GetMapping("/re/re")
    public ResponseEntity<Greeting> responseEntityWithResponseEntityException() {
        throw new HandledResponseEntityException("bad state");
    }

    @GetMapping("/re/pojo")
    public Greeting pojoWithResponseEntityException() {
        throw new HandledResponseEntityException("bad state");
    }

    @GetMapping("/re/void")
    public void voidWithResponseEntityException() {
        throw new HandledResponseEntityException("bad state");
    }

    @GetMapping("/re/void/xml")
    public void voidWithResponseEntityExceptionAsXml() {
        throw new HandledResponseEntityException("bad state", MediaType.APPLICATION_XML);
    }

    @GetMapping("/pojo/re")
    public ResponseEntity responseEntityWithPojoException() {
        throw new HandledPojoException("bad state");
    }

    @GetMapping("/pojo/pojo")
    public Greeting pojoWithPojoException() {
        throw new HandledPojoException("bad state");
    }

    @GetMapping("/pojo/void")
    public void voidWithPojoException() {
        throw new HandledPojoException("bad state");
    }

    @GetMapping("/string")
    public String stringWithStringException() {
        throw new HandledStringException("bad state");
    }
}
