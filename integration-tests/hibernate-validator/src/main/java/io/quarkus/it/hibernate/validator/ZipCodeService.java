package io.quarkus.it.hibernate.validator;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public interface ZipCodeService {

    public String echoZipCode(@NotNull @Size(min = 5, max = 5) String zipCode);

}
