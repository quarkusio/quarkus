package io.quarkus.infinispan.client.runtime.graal;

import java.io.IOException;

import org.infinispan.protostream.SerializationContext;
import org.infinispan.query.remote.client.impl.MarshallerRegistration;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Class that has all the query substitutions necessary to remove code that is loaded when proto marshaller is in use
 * 
 * @author William Burns
 */
final class QuerySubstitutions {
}

@TargetClass(value = MarshallerRegistration.class)
final class SubstituteMarshallerRegistration {
    @Substitute
    public static void init(SerializationContext ctx) throws IOException {
        // Skip loading the proto definition files as this was already done at compile time with
        // HandleProtostreamMarshaller#handleQueryRequirements
        MarshallerRegistration.INSTANCE.registerMarshallers(ctx);
    }
}
