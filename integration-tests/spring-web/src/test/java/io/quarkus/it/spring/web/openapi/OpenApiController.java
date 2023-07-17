package io.quarkus.it.spring.web.openapi;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/resource")
public class OpenApiController {

    @GetMapping
    public String root() {
        return "resource";
    }

    @GetMapping("/test-enums")
    public Query testEnums(@RequestParam(name = "query") Query query) {
        return query;
    }

    public enum Query {
        QUERY_PARAM_1,
        QUERY_PARAM_2;
    }
}
