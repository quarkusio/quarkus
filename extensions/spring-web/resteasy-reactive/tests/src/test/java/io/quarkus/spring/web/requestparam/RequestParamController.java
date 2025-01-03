package io.quarkus.spring.web.requestparam;

import java.util.Map;
import java.util.Optional;

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

    @GetMapping("/api/foosNotParamRequired")
    @ResponseBody
    public String getFoosNotParamRequired2(@RequestParam(required = false) String id) {
        return "ID: " + id;
    }

    @GetMapping("/api/foosOptional")
    @ResponseBody
    public String getFoosOptional(@RequestParam Optional<String> id) {
        return "ID: " + id.orElseGet(() -> "not provided");
    }

    @GetMapping("/api/foosDefaultValue")
    @ResponseBody
    public String getFoosDefaultValue(@RequestParam(defaultValue = "test") String id) {
        return "ID: " + id;
    }

    @PostMapping("/api/foos1")
    @ResponseBody
    public String updateFoos(@RequestParam Map<String, String> allParams) {
        return "Parameters are " + allParams.entrySet();
    }
    //
    //    @GetMapping("/api/foos4")
    //    @ResponseBody
    //    public String getFoos4(@RequestParam List<String> id) {
    //        return "IDs are " + id;
    //    }
    //
    //    @GetMapping("/foos/{id}")
    //    @ResponseBody
    //    public String getFooById(@PathVariable String id) {
    //        return "ID: " + id;
    //    }
    //
    //    @GetMapping("/foos")
    //    @ResponseBody
    //    public String getFooByIdUsingQueryParam(@RequestParam String id) {
    //        return "ID: " + id;
    //    }
    //
    //    @GetMapping({ "/myfoos/optional", "/myfoos/optional/{id}" })
    //    @ResponseBody
    //    public String getFooByOptionalId(@PathVariable(required = false) String id) {
    //        return "ID: " + id;
    //    }
    //
    //    @GetMapping("/myfoos/optionalParam")
    //    @ResponseBody
    //    public String getFooByOptionalIdUsingQueryParam(@RequestParam(required = false) String id) {
    //        return "ID: " + id;
    //    }

}
