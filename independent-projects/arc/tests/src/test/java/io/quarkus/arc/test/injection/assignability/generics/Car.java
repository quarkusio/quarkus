package io.quarkus.arc.test.injection.assignability.generics;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class Car extends Vehicle<PetrolEngine> {
}
