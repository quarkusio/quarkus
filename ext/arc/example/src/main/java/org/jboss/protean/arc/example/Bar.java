package org.jboss.protean.arc.example;

import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Dependent
public class Bar {

    private String name;

    @ConfigProperty(name = "arc.example.surname", defaultValue = "Foo")
    @Inject
    String surname;

    @PostConstruct
    void init() {
        this.name = "Lu";
    }

    @PreDestroy
    void destroy() {
        System.out.println("Destroy bar...");
    }

    public String getName() {
        return name + " " + surname;
    }

    @ApplicationScoped
    @Produces
    List<Number> listOfNumbers() {
        return Collections.emptyList();
    }

}
