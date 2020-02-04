package io.quarkus.amazon.lambda.xray.graal;

import com.amazonaws.xray.contexts.LambdaSegmentContext;
import com.amazonaws.xray.entities.TraceHeader;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.quarkus.amazon.lambda.runtime.TraceId;

@TargetClass(LambdaSegmentContext.class)
public final class LambdaSegmentContextSubstitution {

    @Substitute
    private static TraceHeader getTraceHeaderFromEnvironment() {
        return TraceHeader.fromString(TraceId.getTraceId());
    }

}
