package io.quarkus.infinispan.client.substitutions;

import org.infinispan.client.hotrod.impl.ConfigurationProperties;
import org.infinispan.client.hotrod.marshall.BytesOnlyMarshaller;
import org.infinispan.commons.util.TypedProperties;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * @author William Burns
 */
@TargetClass(ConfigurationProperties.class)
public final class SubstituteConfigurationProperties {
    @Alias
    public static String MARSHALLER = null;
    @Alias
    private TypedProperties props = null;

    @Substitute
    public String getMarshaller() {
        return props.getProperty(MARSHALLER, BytesOnlyMarshaller.class.getName());
    }
}
