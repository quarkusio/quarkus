package io.quarkus.arc.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.util.List;

import io.quarkus.arc.runtime.MockRecorder;
import io.quarkus.deployment.IsTest;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.Record;

@BuildSteps(onlyIf = IsTest.class)
class MockProcessor {
    @BuildStep
    @Record(STATIC_INIT)
    @Consume(BeanContainerBuildItem.class)
    void process(
            MockRecorder mockRecorder,
            List<FieldMockBuildItem> mocks) throws Exception {
        for (FieldMockBuildItem mock : mocks) {
            mockRecorder.registerMock(mock.getDeclaringClass().toString(), mock.getFieldName(), mock.isDeepMocks());
        }
    }
}
