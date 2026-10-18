package org.acme

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("{resource.path}")
class {resource.class-name} {

    @GetMapping
    fun hello() = "{resource.response}"
}