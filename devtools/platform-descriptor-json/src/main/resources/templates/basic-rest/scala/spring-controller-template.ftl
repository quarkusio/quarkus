package ${package_name};

import org.springframework.web.bind.annotation.{GetMapping, RequestMapping, RestController, PathVariable};


@RestController
@RequestMapping(Array[String]("${path}"))
class ${class_name} {

    @GetMapping
    def hello() = "hello"
}
