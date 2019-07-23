package io.quarkus.it.spring.web;

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

}
