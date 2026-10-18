package io.quarkus.it.spring.cache;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cache")
public class SpringCacheController {

    final CachedGreetingService cachedGreetingService;

    public SpringCacheController(CachedGreetingService cachedGreetingService) {
        this.cachedGreetingService = cachedGreetingService;
    }

    @GetMapping(path = "/greet/{name}")
    public Greeting greet(@PathVariable String name) {
        return cachedGreetingService.greet(name);
    }
}
