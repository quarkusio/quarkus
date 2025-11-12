package io.quarkus.test.reload;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AddressMapper {

    public String map(AddressData address) {
        return address.name1;
    }
}
