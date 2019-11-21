package io.quarkus.it.spring.security.security;

import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

@Service("subclassService")
@Secured("admin")
public class Subclass extends ServiceWithSecuredMethods {
}
