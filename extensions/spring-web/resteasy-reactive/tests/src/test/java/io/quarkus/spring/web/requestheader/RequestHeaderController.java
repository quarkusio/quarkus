package io.quarkus.spring.web.requestheader;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class RequestHeaderController {

    @GetMapping("/api/header")
    @ResponseBody
    public String getHeader(@RequestHeader("X-Custom-Header") String value) {
        return "Header: " + value;
    }

    @GetMapping("/api/header/default")
    @ResponseBody
    public String getHeaderWithDefault(@RequestHeader(name = "X-Optional-Header", defaultValue = "fallback") String value) {
        return "Header: " + value;
    }
}
