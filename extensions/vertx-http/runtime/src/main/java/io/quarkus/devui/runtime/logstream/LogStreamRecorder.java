package io.quarkus.devui.runtime.logstream;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class LogStreamRecorder {

    public RuntimeValue<Optional<MutinyLogHandler>> mutinyLogHandler(boolean decorateStack, String srcMainJava,
            List<String> knownClasses) {
        return new RuntimeValue<>(Optional.of(new MutinyLogHandler(decorateStack, srcMainJava, knownClasses)));
    }

}
