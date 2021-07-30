package io.quarkus.amazon.lambda.http.graal;

import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(LambdaContainerHandler.class)
public class LambdaContainerHandlerSubstitution {

    // afterburner does not work in native mode, so let's ensure it's never registered
    @Substitute
    private static void registerAfterBurner() {

    }
}
