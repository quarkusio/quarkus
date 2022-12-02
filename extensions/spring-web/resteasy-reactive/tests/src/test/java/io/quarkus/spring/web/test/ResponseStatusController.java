package io.quarkus.spring.web.test;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/" + ResponseStatusController.CONTROLLER_PATH)
public class ResponseStatusController {

    public static final String CONTROLLER_PATH = "rs";

    @GetMapping(produces = "text/plain", path = "/noContent")
    @ResponseStatus(HttpStatus.OK)
    public void noValueResponseStatus() {

    }

    @GetMapping(produces = "text/plain", path = "/string")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public String stringWithResponseStatus() {
        return "accepted";
    }
}
