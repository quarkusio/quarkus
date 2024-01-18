package io.quarkus.extest.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.extest.runtime.records.TestRecord;
import io.quarkus.extest.runtime.records.TestRecordRecorder;

public class TestRecordProcessor {

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void record(TestRecordRecorder recorder) {
        recorder.record(new TestRecord("foo", 100));
    }
}
