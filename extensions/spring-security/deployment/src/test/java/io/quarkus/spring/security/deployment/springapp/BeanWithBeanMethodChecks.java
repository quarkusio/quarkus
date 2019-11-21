package io.quarkus.spring.security.deployment.springapp;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class BeanWithBeanMethodChecks {

    @PreAuthorize("@personChecker.isTrue()")
    public String noParamsAlwaysPasses() {
        return "noParamsAlwaysPasses";
    }

    @PreAuthorize("@personChecker.isFalse()")
    public String noParamsNeverPasses() {
        return "noParamsNeverPasses";
    }

    @PreAuthorize("@personChecker.check(#person, #input)")
    public String withParams(String input, Person person) {
        return "withParams";
    }

    @PreAuthorize("@personChecker.check(#person, #input)")
    public String anotherWithParams(String input, Person person) {
        return "anotherWithParams";
    }

    @PreAuthorize("@principalChecker.isSame(#input, authentication.principal.username)")
    public String principalChecker(String input) {
        return "principalChecker";
    }
}
