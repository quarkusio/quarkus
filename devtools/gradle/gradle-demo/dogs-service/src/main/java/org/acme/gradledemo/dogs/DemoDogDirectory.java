package org.acme.gradledemo.dogs;

import java.util.List;

import org.acme.gradledemo.api.Dog;
import org.acme.gradledemo.api.DogDirectory;

public class DemoDogDirectory implements DogDirectory {

    @Override
    public List<Dog> dogs() {
        return List.of(new Dog("Milo"), new Dog("Penny"));
    }
}
