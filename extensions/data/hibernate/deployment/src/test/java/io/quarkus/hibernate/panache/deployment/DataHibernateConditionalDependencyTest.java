/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package io.quarkus.hibernate.panache.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.jar.JarFile;

import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.hibernate.panache.runtime.hr.ManagedReactiveJpaOperations;

public class DataHibernateConditionalDependencyTest {

    @Test
    public void declaresHibernateReactivePanacheCommonAsConditionalDependency() throws IOException {
        URL location = ManagedReactiveJpaOperations.class.getProtectionDomain().getCodeSource().getLocation();
        try (JarFile jar = new JarFile(location.getFile())) {
            var entry = jar.getJarEntry("META-INF/quarkus-extension.properties");
            assertThat(entry).as("quarkus-data-hibernate extension descriptor").isNotNull();
            Properties properties = new Properties();
            try (InputStream in = jar.getInputStream(entry)) {
                properties.load(in);
            }
            assertThat(properties.getProperty(BootstrapConstants.CONDITIONAL_DEPENDENCIES))
                    .contains("io.quarkus:quarkus-hibernate-reactive-panache-common:");
        }
    }
}
