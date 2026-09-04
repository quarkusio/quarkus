package io.quarkus.spring.web.requestheader;

import java.util.Map;
import java.util.TreeMap;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/api/headers/all")
    public String getAllHeaders(@RequestHeader Map<String, String> headers) {
        Map<String, String> sorted = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        sorted.putAll(headers);
        return "Headers: " + sorted;
    }

    @GetMapping("/api/headers/namedByName")
    public String getNamedByNameHeader(@RequestHeader(name = "X-Other-Header") String otherHeader) {
        return "Other-Header: " + otherHeader;
    }

    @GetMapping("/api/headers/mixed/{id}")
    public String mixedParams(
            @PathVariable String id,
            @RequestParam String filter,
            @RequestHeader("X-Token") String token,
            @RequestHeader Map<String, String> allHeaders) {
        return "id=" + id + " filter=" + filter + " token=" + token + " hasHeaders=" + !allHeaders.isEmpty();
    }

}
