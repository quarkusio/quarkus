package ${package_name};

import org.springframework.web.bind.annotation.{GetMapping, RequestMapping, RestController};


@RestController
@RequestMapping(Array[String]("${path}"))
class ${class_name} {

    @GetMapping
    def hello() = "hello"
}
