package io.quarkus.spring.web.requestparam;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class RequestParamController {

    @GetMapping("/api/foos")
    @ResponseBody
    public String getFoos(@RequestParam String id) {
        return "ID: " + id;
    }

    @PostMapping("/api/foos")
    @ResponseBody
    public String addFoo(@RequestParam(name = "id") String fooId, @RequestParam String name) {
        return "ID: " + fooId + " Name: " + name;
    }

    @GetMapping("/api/foos/notParamRequired")
    @ResponseBody
    public String getFoosNotParamRequired2(@RequestParam(required = false) String id) {
        return "ID: " + id;
    }

    @GetMapping("/api/foos/optional")
    @ResponseBody
    public String getFoosOptional(@RequestParam Optional<String> id) {
        return "ID: " + id.orElseGet(() -> "not provided");
    }

    @GetMapping("/api/foos/defaultValue")
    @ResponseBody
    public String getFoosDefaultValue(@RequestParam(defaultValue = "test") String id) {
        return "ID: " + id;
    }

    @PostMapping("/api/foos/map")
    @ResponseBody
    public String updateFoosMap(@RequestParam Map<String, String> allParams) {
        return "Parameters are " + allParams.entrySet();
    }

    @GetMapping("/api/foos/multivalue")
    @ResponseBody
    public String getFoosMultiValue(@RequestParam List<String> id) {
        return "IDs are " + id;
    }

    @PostMapping("/api/foos/multiMap")
    @ResponseBody
    public String updateFoos(@RequestParam MultiValueMap<String, String> allParams) {
        String result = "";
        for (Map.Entry<String, List<String>> entry : allParams.entrySet()) {
            result = "Parameters are " + entry.getKey() + "=" + entry.getValue().stream().collect(Collectors.joining(", "));
        }
        return result;
    }

}
