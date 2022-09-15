package io.quarkus.arc.processor.types;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.util.List;

@Dependent
public class Baz {

    @Inject
    Instance<List<String>> list;

    public boolean isListResolvable() {
        return list.isResolvable();
    }

}
