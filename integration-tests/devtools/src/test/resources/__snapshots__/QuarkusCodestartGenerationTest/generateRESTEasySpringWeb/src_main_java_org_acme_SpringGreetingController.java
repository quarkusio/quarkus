package org.acme;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hello-spring")
public class SpringGreetingController {

    @GetMapping
    public String hello() {
        return "Hello Spring";
    }
}
