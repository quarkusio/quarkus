package ${package};

import jakarta.inject.Inject;
import io.quarkus.funqy.Funq;

public class UnusedFunq {

    @Inject
    ProcessingService service;

    @Funq
    public OutputObject unused(InputObject input) {
        throw new RuntimeException("Should be unused");
    }
}
