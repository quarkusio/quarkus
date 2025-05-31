/**
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package io.quarkus.kubernetes.spi;

import static io.quarkus.kubernetes.spi.KubernetesEnvBuildItem.create;
import static io.quarkus.kubernetes.spi.KubernetesEnvBuildItem.EnvType.configmap;
import static io.quarkus.kubernetes.spi.KubernetesEnvBuildItem.EnvType.secret;
import static io.quarkus.kubernetes.spi.KubernetesEnvBuildItem.EnvType.var;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * @author <a href="claprun@redhat.com">Christophe Laprun</a>
 */
public class KubernetesEnvBuildItemTest {

    private static final String TARGET = "target";
    private static final String VALUE = "value";
    private static final String NAME = "name";
    private static final String PREFIX = "prefix";

    @Test
    public void testCreateSimpleVarFromEnvConfig() {
        final KubernetesEnvBuildItem item = create(NAME, VALUE, null, null, null, TARGET, null);
        assertEquals(var, item.getType());
        assertEquals(NAME, item.getName());
        assertEquals(VALUE, item.getValue());
        assertEquals(TARGET, item.getTarget());
        assertNull(item.getConfigMap());
        assertNull(item.getSecret());
        assertNull(item.getField());
    }

    @Test
    public void testCreateLoadFromConfigMapFromEnvConfig() {
        final KubernetesEnvBuildItem item = create(NAME, null, null, VALUE, null, TARGET, null);
        assertEquals(configmap, item.getType());
        assertEquals(VALUE, item.getName());
        assertNull(item.getValue());
        assertEquals(VALUE, item.getConfigMap());
        assertNull(item.getSecret());
        assertNull(item.getField());
    }

    @Test
    public void testCreateConfigMapWithPrefix() {
        final KubernetesEnvBuildItem item = create(NAME, null, null, VALUE, null, TARGET, PREFIX);
        assertEquals(configmap, item.getType());
        assertEquals(VALUE, item.getName());
        assertNull(item.getValue());
        assertEquals(VALUE, item.getConfigMap());
        assertNull(item.getSecret());
        assertNull(item.getField());
        assertEquals(PREFIX, item.getPrefix());
    }

    @Test
    public void testCreateSecretWithPrefix() {
        final KubernetesEnvBuildItem item = create(NAME, null, VALUE, null, null, TARGET, PREFIX);
        assertEquals(secret, item.getType());
        assertEquals(VALUE, item.getName());
        assertNull(item.getValue());
        assertEquals(VALUE, item.getSecret());
        assertNull(item.getConfigMap());
        assertNull(item.getField());
        assertEquals(PREFIX, item.getPrefix());
    }
}
