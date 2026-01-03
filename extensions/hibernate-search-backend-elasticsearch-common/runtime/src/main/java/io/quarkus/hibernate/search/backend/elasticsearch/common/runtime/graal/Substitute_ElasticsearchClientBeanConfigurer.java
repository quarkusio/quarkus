package io.quarkus.hibernate.search.backend.elasticsearch.common.runtime.graal;

import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchClientFactory;
import org.hibernate.search.backend.elasticsearch.client.impl.ClientJdkElasticsearchClientFactory;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurationContext;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.hibernate.search.backend.elasticsearch.client.impl.ElasticsearchClientBeanConfigurer")
final class Substitute_ElasticsearchClientBeanConfigurer {

    @Substitute
    public void configure(BeanConfigurationContext context) {
        context.define(ElasticsearchClientFactory.class, "jdk-rest",
                new BeanReference<>() {
                    @Override
                    public BeanHolder<ElasticsearchClientFactory> resolve(BeanResolver beanResolver) {
                        return BeanHolder.of(new ClientJdkElasticsearchClientFactory());
                    }
                });
    }
}
