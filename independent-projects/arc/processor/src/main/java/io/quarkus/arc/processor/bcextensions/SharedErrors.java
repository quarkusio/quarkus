package io.quarkus.arc.processor.bcextensions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class SharedErrors {
    final List<Throwable> list = Collections.synchronizedList(new ArrayList<>());

    void add(Throwable exception) {
        list.add(exception);
    }
}
