package io.quarkus.infinispan.client.runtime.graal;

import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.marshall.BytesOnlyMarshaller;
import org.infinispan.commons.marshall.Marshaller;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * We only support byte[] by default for marshalling
 * 
 * @author William Burns
 */
@TargetClass(ConfigurationBuilder.class)
public final class SubstituteConfigurationBuilder {

    @Substitute
    private Marshaller handleNullMarshaller() {
        return BytesOnlyMarshaller.INSTANCE;
    }
}
