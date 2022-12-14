package io.quarkus.runtime;

import java.util.List;

import io.quarkus.runtime.annotations.ReadOnly;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class BlockingOperationRecorder {

    public void control(@ReadOnly List<IOThreadDetector> detectors) {
        BlockingOperationControl.setIoThreadDetector(detectors.toArray(new IOThreadDetector[0]));
    }
}
