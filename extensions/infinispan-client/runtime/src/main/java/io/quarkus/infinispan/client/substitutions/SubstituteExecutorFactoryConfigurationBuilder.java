package io.quarkus.infinispan.client.substitutions;

import java.util.Properties;

import org.infinispan.client.hotrod.configuration.ExecutorFactoryConfiguration;
import org.infinispan.client.hotrod.configuration.ExecutorFactoryConfigurationBuilder;
import org.infinispan.client.hotrod.impl.async.DefaultAsyncExecutorFactory;
import org.infinispan.commons.executors.ExecutorFactory;
import org.infinispan.commons.util.TypedProperties;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Avoids using reflection for DefaultAsyncExecutorFactory class
 * 
 * @author William Burns
 */
@TargetClass(ExecutorFactoryConfigurationBuilder.class)
public final class SubstituteExecutorFactoryConfigurationBuilder {
    @Alias
    private ExecutorFactory factory;
    @Alias
    private Properties properties;

    @Substitute
    public SubstituteExecutorFactoryConfiguration create() {
        if (factory != null)
            return new SubstituteExecutorFactoryConfiguration(factory, TypedProperties.toTypedProperties(properties));
        else
            return new SubstituteExecutorFactoryConfiguration(new DefaultAsyncExecutorFactory(),
                    TypedProperties.toTypedProperties(properties));
    }
}

@TargetClass(ExecutorFactoryConfiguration.class)
final class SubstituteExecutorFactoryConfiguration {
    @Alias
    SubstituteExecutorFactoryConfiguration(ExecutorFactory factory, TypedProperties properties) {
    }

}
