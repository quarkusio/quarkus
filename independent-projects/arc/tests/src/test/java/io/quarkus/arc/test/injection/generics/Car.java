package io.quarkus.arc.test.injection.generics;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class Car extends Vehicle<PetrolEngine> {
}
