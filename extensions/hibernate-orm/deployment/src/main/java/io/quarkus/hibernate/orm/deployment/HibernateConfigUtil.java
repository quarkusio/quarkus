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

package io.quarkus.hibernate.orm.deployment;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.hibernate.orm.deployment.HibernateOrmConfig.HibernateOrmConfigCache;

public class HibernateConfigUtil {

    /**
     * TODO: reuse the ones from QuarkusInfinispanRegionFactory as soon as they are made public.
     */
    private final static String EXPIRATION_MAX_IDLE = ".expiration.max-idle";
    private final static String MEMORY_OBJECT_COUNT = ".memory.object-count";
    private static final String HIBERNATE_CACHE_PREFIX = "hibernate.cache.";

    public static Map<String, String> getCacheConfigEntries(HibernateOrmConfig config) {
        Map<String, String> cacheRegionsConfigEntries = new HashMap<>();
        for (Map.Entry<String, HibernateOrmConfigCache> regionEntry : config.cache.entrySet()) {
            String regionName = regionEntry.getKey();
            HibernateOrmConfigCache cacheConfig = regionEntry.getValue();

            if (cacheConfig.expiration.maxIdle.isPresent()) {
                cacheRegionsConfigEntries.put(getCacheConfigKey(regionName, EXPIRATION_MAX_IDLE),
                        String.valueOf(cacheConfig.expiration.maxIdle.getAsLong()));
            }
            if (cacheConfig.memory.objectCount.isPresent()) {
                cacheRegionsConfigEntries.put(getCacheConfigKey(regionName, MEMORY_OBJECT_COUNT),
                        String.valueOf(cacheConfig.memory.objectCount.getAsLong()));
            }
        }

        return cacheRegionsConfigEntries;
    }

    private static String getCacheConfigKey(String regionName, String configKey) {
        return HIBERNATE_CACHE_PREFIX + regionName + configKey;
    }
}
