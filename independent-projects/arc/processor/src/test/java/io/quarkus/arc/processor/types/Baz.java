package io.quarkus.arc.processor.types;

import java.util.List;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

@Dependent
public class Baz {

    @Inject
    Instance<List<String>> list;

    public boolean isListResolvable() {
        return list.isResolvable();
    }

}
