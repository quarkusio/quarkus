package org.acme;

import java.util.List;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class AcmeQuarkusExtRecorder {

    public RuntimeValue<ModuleList> initLocalModules(List<String> localModules) {
        return new RuntimeValue<>(new ModuleList(localModules));
    }
}
