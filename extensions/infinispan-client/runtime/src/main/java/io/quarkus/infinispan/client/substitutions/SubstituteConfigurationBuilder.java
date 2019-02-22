package io.quarkus.infinispan.client.substitutions;

import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.marshall.BytesOnlyMarshaller;
import org.infinispan.commons.marshall.Marshaller;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * We only support byte[] by default for marshalling
 * 
 * @author William Burns
 */
@TargetClass(ConfigurationBuilder.class)
public final class SubstituteConfigurationBuilder {
    @Alias
    private Marshaller marshaller;

    @Substitute
    private void handleNullMarshaller() {
        marshaller = BytesOnlyMarshaller.INSTANCE;
    }
}
