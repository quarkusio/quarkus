package io.quarkus.spring.security.deployment.springapp;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
public class SomeInterfaceImpl implements SomeInterface {

    @Override
    @PreAuthorize("@personChecker.check(#person, #input)")
    public String doSomething(String input, int num, Person person) {
        return "doSomething";
    }
}
