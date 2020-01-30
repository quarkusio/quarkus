package io.quarkus.it.mongodb.panache.bugs;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class Bug6324ConcreteRepository extends Bug6324AbstractRepository<NeedReflection> {
}
