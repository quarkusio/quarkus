package ${package};

import jakarta.inject.Inject;
import io.quarkus.funqy.Funq;

public class TestFunq {

    @Inject
    ProcessingService service;

    @Funq
    public OutputObject greeting(InputObject input) {
        return service.process(input);
    }
}
