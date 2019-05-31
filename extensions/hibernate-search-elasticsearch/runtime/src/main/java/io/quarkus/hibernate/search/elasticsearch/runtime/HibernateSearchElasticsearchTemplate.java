/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.hibernate.search.elasticsearch.runtime;

import static io.quarkus.hibernate.search.elasticsearch.runtime.HibernateSearchConfigUtil.addBackendConfig;
import static io.quarkus.hibernate.search.elasticsearch.runtime.HibernateSearchConfigUtil.addBackendDefaultIndexConfig;
import static io.quarkus.hibernate.search.elasticsearch.runtime.HibernateSearchConfigUtil.addBackendIndexConfig;
import static io.quarkus.hibernate.search.elasticsearch.runtime.HibernateSearchConfigUtil.addConfig;

import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.engine.cfg.BackendSettings;
import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.mapper.orm.bootstrap.spi.HibernateOrmIntegrationBooter;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.cfg.spi.HibernateOrmMapperSpiSettings;
import org.hibernate.search.mapper.orm.cfg.spi.HibernateOrmPropertyHandleFactoryName;

import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationListener;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrations;
import io.quarkus.hibernate.search.elasticsearch.runtime.HibernateSearchElasticsearchBuildTimeConfig.ElasticsearchBackendBuildTimeConfig;
import io.quarkus.hibernate.search.elasticsearch.runtime.HibernateSearchElasticsearchRuntimeConfig.ElasticsearchBackendRuntimeConfig;
import io.quarkus.hibernate.search.elasticsearch.runtime.HibernateSearchElasticsearchRuntimeConfig.ElasticsearchIndexConfig;
import io.quarkus.runtime.annotations.Template;

@Template
public class HibernateSearchElasticsearchTemplate {

    public static final String DEFAULT_BACKEND = "_quarkus_";

    private static HibernateSearchElasticsearchRuntimeConfig runtimeConfig;

    public void registerHibernateSearchIntegration(HibernateSearchElasticsearchBuildTimeConfig buildTimeConfig) {
        HibernateOrmIntegrations.registerListener(new HibernateSearchIntegrationListener(buildTimeConfig));
    }

    public void setRuntimeConfig(HibernateSearchElasticsearchRuntimeConfig runtimeConfig) {
        HibernateSearchElasticsearchTemplate.runtimeConfig = runtimeConfig;
    }

    private static final class HibernateSearchIntegrationListener implements HibernateOrmIntegrationListener {

        private HibernateSearchElasticsearchBuildTimeConfig buildTimeConfig;

        private HibernateSearchIntegrationListener(HibernateSearchElasticsearchBuildTimeConfig buildTimeConfig) {
            this.buildTimeConfig = buildTimeConfig;
        }

        @Override
        public void contributeBootProperties(BiConsumer<String, Object> propertyCollector) {
            addConfig(propertyCollector, HibernateOrmMapperSpiSettings.Radicals.PROPERTY_HANDLE_FACTORY,
                    HibernateOrmPropertyHandleFactoryName.JAVA_LANG_REFLECT);

            if (buildTimeConfig.defaultBackend.isPresent()) {
                // we have a named default backend
                addConfig(propertyCollector, EngineSettings.DEFAULT_BACKEND,
                        buildTimeConfig.defaultBackend.get());
            } else if (buildTimeConfig.elasticsearch.version.isPresent()) {
                // we use the default backend configuration
                addConfig(propertyCollector, EngineSettings.DEFAULT_BACKEND,
                        HibernateSearchElasticsearchTemplate.DEFAULT_BACKEND);
            }

            contributeBackendBuildTimeProperties(propertyCollector, HibernateSearchElasticsearchTemplate.DEFAULT_BACKEND,
                    buildTimeConfig.elasticsearch);

            for (Entry<String, ElasticsearchBackendBuildTimeConfig> backendEntry : buildTimeConfig.additionalBackends
                    .entrySet()) {
                contributeBackendBuildTimeProperties(propertyCollector, backendEntry.getKey(), backendEntry.getValue());
            }
        }

        @Override
        public void onMetadataInitialized(Metadata metadata, BootstrapContext bootstrapContext,
                BiConsumer<String, Object> propertyCollector) {
            HibernateOrmIntegrationBooter booter = HibernateOrmIntegrationBooter.create(metadata, bootstrapContext);
            booter.preBoot(propertyCollector);
        }

        @Override
        public void contributeRuntimeProperties(BiConsumer<String, Object> propertyCollector) {
            addConfig(propertyCollector,
                    HibernateOrmMapperSettings.Radicals.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY,
                    runtimeConfig.elasticsearch.automaticIndexing.synchronizationStrategy);
            addConfig(propertyCollector,
                    HibernateOrmMapperSettings.Radicals.AUTOMATIC_INDEXING_ENABLE_DIRTY_CHECK,
                    runtimeConfig.elasticsearch.automaticIndexing.enableDirtyCheck);

            contributeBackendRuntimeProperties(propertyCollector, DEFAULT_BACKEND, runtimeConfig.elasticsearch);

            for (Entry<String, ElasticsearchBackendRuntimeConfig> backendEntry : runtimeConfig.additionalBackends.entrySet()) {
                contributeBackendRuntimeProperties(propertyCollector, backendEntry.getKey(), backendEntry.getValue());
            }
        }

        private void contributeBackendBuildTimeProperties(BiConsumer<String, Object> propertyCollector, String backendName,
                ElasticsearchBackendBuildTimeConfig elasticsearchBackendConfig) {
            addBackendConfig(propertyCollector, backendName, BackendSettings.TYPE,
                    ElasticsearchBackendSettings.TYPE_NAME);
            addBackendConfig(propertyCollector, backendName, ElasticsearchBackendSettings.VERSION,
                    elasticsearchBackendConfig.version);
            addBackendConfig(propertyCollector, backendName,
                    ElasticsearchBackendSettings.ANALYSIS_CONFIGURER,
                    elasticsearchBackendConfig.analysisConfigurer,
                    Optional::isPresent, c -> c.get().getName());
        }

        private void contributeBackendRuntimeProperties(BiConsumer<String, Object> propertyCollector, String backendName,
                ElasticsearchBackendRuntimeConfig elasticsearchBackendConfig) {
            addBackendConfig(propertyCollector, backendName, ElasticsearchBackendSettings.HOSTS,
                    elasticsearchBackendConfig.hosts,
                    v -> (!v.isEmpty() && !(v.size() == 1 && v.get(0).isEmpty())), Function.identity());
            addBackendConfig(propertyCollector, backendName, ElasticsearchBackendSettings.USERNAME,
                    elasticsearchBackendConfig.username);
            addBackendConfig(propertyCollector, backendName, ElasticsearchBackendSettings.PASSWORD,
                    elasticsearchBackendConfig.password);
            addBackendConfig(propertyCollector, backendName, ElasticsearchBackendSettings.CONNECTION_TIMEOUT,
                    elasticsearchBackendConfig.connectionTimeout,
                    Optional::isPresent, d -> d.get().toMillis());
            addBackendConfig(propertyCollector, backendName, ElasticsearchBackendSettings.MAX_CONNECTIONS,
                    elasticsearchBackendConfig.maxConnections);
            addBackendConfig(propertyCollector, backendName, ElasticsearchBackendSettings.MAX_CONNECTIONS_PER_ROUTE,
                    elasticsearchBackendConfig.maxConnectionsPerRoute);

            addBackendConfig(propertyCollector, backendName, ElasticsearchBackendSettings.DISCOVERY_ENABLED,
                    elasticsearchBackendConfig.discovery.enabled);
            addBackendConfig(propertyCollector, backendName, ElasticsearchBackendSettings.DISCOVERY_REFRESH_INTERVAL,
                    elasticsearchBackendConfig.discovery.refreshInterval,
                    Optional::isPresent, d -> d.get().getSeconds());
            addBackendConfig(propertyCollector, backendName, ElasticsearchBackendSettings.DISCOVERY_SCHEME,
                    elasticsearchBackendConfig.discovery.defaultScheme);

            addBackendDefaultIndexConfig(propertyCollector, backendName, ElasticsearchIndexSettings.LIFECYCLE_STRATEGY,
                    elasticsearchBackendConfig.indexDefaults.lifecycle.strategy);
            addBackendDefaultIndexConfig(propertyCollector, backendName,
                    ElasticsearchIndexSettings.LIFECYCLE_MINIMAL_REQUIRED_STATUS,
                    elasticsearchBackendConfig.indexDefaults.lifecycle.requiredStatus);
            addBackendDefaultIndexConfig(propertyCollector, backendName,
                    ElasticsearchIndexSettings.LIFECYCLE_MINIMAL_REQUIRED_STATUS_WAIT_TIMEOUT,
                    elasticsearchBackendConfig.indexDefaults.lifecycle.requiredStatusWaitTimeout, Optional::isPresent,
                    d -> d.get().toMillis());

            for (Entry<String, ElasticsearchIndexConfig> indexConfigEntry : runtimeConfig.elasticsearch.indexes.entrySet()) {
                String indexName = indexConfigEntry.getKey();
                ElasticsearchIndexConfig indexConfig = indexConfigEntry.getValue();

                addBackendIndexConfig(propertyCollector, backendName, indexName, ElasticsearchIndexSettings.LIFECYCLE_STRATEGY,
                        indexConfig.lifecycle.strategy);
                addBackendIndexConfig(propertyCollector, backendName, indexName,
                        ElasticsearchIndexSettings.LIFECYCLE_MINIMAL_REQUIRED_STATUS,
                        indexConfig.lifecycle.requiredStatus);
                addBackendIndexConfig(propertyCollector, backendName, indexName,
                        ElasticsearchIndexSettings.LIFECYCLE_MINIMAL_REQUIRED_STATUS_WAIT_TIMEOUT,
                        indexConfig.lifecycle.requiredStatusWaitTimeout, Optional::isPresent,
                        d -> d.get().toMillis());
            }
        }
    }
}
