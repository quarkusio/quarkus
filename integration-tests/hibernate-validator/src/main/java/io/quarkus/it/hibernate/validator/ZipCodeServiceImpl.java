package io.quarkus.it.hibernate.validator;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ZipCodeServiceImpl implements ZipCodeService {

    @Override
    public String echoZipCode(String zipCode) {
        return zipCode;
    }
}
