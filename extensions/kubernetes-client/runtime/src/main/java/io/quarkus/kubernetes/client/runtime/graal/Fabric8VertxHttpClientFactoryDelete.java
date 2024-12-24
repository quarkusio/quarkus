package io.quarkus.kubernetes.client.runtime.graal;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "io.fabric8.kubernetes.client.vertx.VertxHttpClientFactory")
@Delete
public final class Fabric8VertxHttpClientFactoryDelete {
}
