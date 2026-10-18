package io.quarkus.vertx;

public class MyPetCodec extends LocalEventBusCodec<Pet> {

    @Override
    public Pet transform(Pet pet) {
        return new Pet(pet.getName().toUpperCase(), pet.getKind());
    }

    @Override
    public String name() {
        return MyPetCodec.class.getName();
    }
}
