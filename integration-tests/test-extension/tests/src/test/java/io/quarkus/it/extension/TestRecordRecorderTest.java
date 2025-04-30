package io.quarkus.it.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.quarkus.extest.runtime.records.TestRecordRecorder;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class TestRecordRecorderTest {

    @Test
    public void test() {
        assertEquals("foo", TestRecordRecorder.testRecord.name());
        assertEquals(100, TestRecordRecorder.testRecord.age());
    }
}
