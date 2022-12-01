package ilove.quark.us

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/greeting")
class GreetingController {

    @GetMapping
    fun hello() = "Hello Spring"
}