package io.quarkus.arc.test.decorators.other;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ToUpperCaseConverter implements Converter<String> {

    @Override
    public String convert(String value) {
        return value.toUpperCase();
    }

    public String convertNoDelegation(String value) {
        return value.toUpperCase();
    }

}
