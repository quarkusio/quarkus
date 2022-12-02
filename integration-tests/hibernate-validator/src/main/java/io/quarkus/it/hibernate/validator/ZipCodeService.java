package io.quarkus.it.hibernate.validator;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public interface ZipCodeService {

    public String echoZipCode(@NotNull @Size(min = 5, max = 5) String zipCode);

}
