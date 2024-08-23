package io.quarkus.extest.runtime.records;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class TestRecordRecorder {

    public static TestRecord testRecord;

    public void record(TestRecord testRecord) {
        TestRecordRecorder.testRecord = testRecord;
    }
}
