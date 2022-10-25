package io.quarkus.arc.processor.types;

import java.util.AbstractList;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;

@Dependent
@Default
@FooQualifier(alpha = "ignored", bravo = "no")
public class Foo extends AbstractList<String> {

    @PreDestroy
    void superCoolDestroyCallback() {
    }

    @Override
    public String get(int index) {
        return null;
    }

    @Override
    public int size() {
        return 0;
    }

}
