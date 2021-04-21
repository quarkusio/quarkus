package ilove.quark.us;

import org.springframework.web.bind.annotation.{GetMapping, RequestMapping, RestController}


@RestController
@RequestMapping(Array[String]("/greeting"))
class GreetingController {

    @GetMapping
    def hello() = "Hello Spring"
}