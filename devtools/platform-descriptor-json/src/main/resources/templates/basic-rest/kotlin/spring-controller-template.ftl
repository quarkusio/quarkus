package ${package_name};

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("${path}")
class ${class_name} {

    @GetMapping
    fun hello() = "hello"
}
