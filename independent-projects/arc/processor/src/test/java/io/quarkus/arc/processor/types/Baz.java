package io.quarkus.arc.processor.types;

import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@Dependent
public class Baz {

    @Inject
    Instance<List<String>> list;

    public boolean isListResolvable() {
        return list.isResolvable();
    }

}
