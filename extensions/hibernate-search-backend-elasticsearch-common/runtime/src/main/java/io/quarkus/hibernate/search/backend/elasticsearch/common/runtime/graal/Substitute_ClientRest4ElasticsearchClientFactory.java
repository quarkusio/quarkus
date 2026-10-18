package io.quarkus.hibernate.search.backend.elasticsearch.common.runtime.graal;

import org.hibernate.search.backend.elasticsearch.client.common.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchClientImplementor;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.common.execution.spi.SimpleScheduledExecutor;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.thread.spi.ThreadProvider;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

// Same as with the Client itself, we just remove all possible references to Apache 4 client classes
// that GraalVM thinks might be reachable. They are not -- we rely on a ClientRest5ElasticsearchClient/ClientRest5ElasticsearchClientFactory.
// TODO: With Apache 4 client removed from dependencies of the backend, this would go away.
@TargetClass(className = "org.hibernate.search.backend.elasticsearch.client.impl.ClientRest4ElasticsearchClientFactory")
final class Substitute_ClientRest4ElasticsearchClientFactory {

    @Substitute
    public ElasticsearchClientImplementor create(BeanResolver beanResolver,
            ConfigurationPropertySource configurationPropertySource, ThreadProvider threadProvider, String s,
            SimpleScheduledExecutor simpleScheduledExecutor, GsonProvider gsonProvider) {
        throw new UnsupportedOperationException();
    }
}
