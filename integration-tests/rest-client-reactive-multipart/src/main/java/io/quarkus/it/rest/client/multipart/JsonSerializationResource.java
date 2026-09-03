package io.quarkus.it.rest.client.multipart;

import io.quarkus.it.rest.client.multipart.model.ContainerDTO;
import io.quarkus.it.rest.client.multipart.model.Dog;
import io.quarkus.it.rest.client.multipart.model.NestedInterface;

public class JsonSerializationResource extends AbstractJsonSerializationResource {

    @Override
    public Dog echoDog(Dog dog) {
        return dog;
    }

    @Override
    public ContainerDTO interfaceTest() {
        return new ContainerDTO(NestedInterface.INSTANCE);
    }
}
