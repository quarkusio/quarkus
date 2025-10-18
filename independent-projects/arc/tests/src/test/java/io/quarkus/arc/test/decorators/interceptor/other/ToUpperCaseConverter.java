package io.quarkus.arc.test.decorators.interceptor.other;

import jakarta.enterprise.context.ApplicationScoped;

@Logging
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
