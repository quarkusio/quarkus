package io.quarkus.it.spring;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class UseNamedBeanService {

    String name;

    public UseNamedBeanService(@Qualifier("mySpecialName") String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
