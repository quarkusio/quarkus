package io.quarkus.it.hibernate.validator.injection;

import java.util.regex.Pattern;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MyRuntimeService {

    private final Pattern pattern;

    @Inject
    public MyRuntimeService(MyRuntimeConfiguration configuration) {
        this.pattern = Pattern.compile(configuration.pattern());
    }

    public boolean validate(String value) {
        return pattern.matcher(value).matches();
    }
}
