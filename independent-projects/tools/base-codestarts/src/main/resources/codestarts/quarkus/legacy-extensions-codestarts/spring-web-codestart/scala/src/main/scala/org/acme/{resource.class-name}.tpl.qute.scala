package org.acme;

import org.springframework.web.bind.annotation.\{GetMapping, RequestMapping, RestController}


@RestController
@RequestMapping(Array[String]("{resource.path}"))
class {resource.class-name} {

    @GetMapping
    def hello() = "{resource.response}"
}