package io.quarkus.jaeger.runtime.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.jaegertracing.Configuration;
import io.jaegertracing.spi.Sender;
import io.jaegertracing.thrift.internal.senders.ThriftSenderFactory;

@TargetClass(className = "io.jaegertracing.internal.senders.SenderResolver")
public final class Target_SenderResolver {

    @Substitute
    public static Sender resolve(Configuration.SenderConfiguration senderConfiguration) {
        return new ThriftSenderFactory().getSender(senderConfiguration);
    }
}
