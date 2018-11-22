/*
 * Copyright 2018 Red Hat, Inc.
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

package org.jboss.shamrock.beanvalidation.runtime.graal;

import static org.hibernate.validator.internal.util.CollectionHelper.newHashSet;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

import org.hibernate.validator.internal.util.privilegedactions.GetResources;
import org.hibernate.validator.resourceloading.PlatformResourceBundleLocator;
import org.jboss.logging.Logger;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(PlatformResourceBundleLocator.class)
final class PlatformResourceBundleLocatorSubstitution {
    @Alias
    private static final Logger log = null;
    @Alias
    private final String bundleName = null;
    @Alias
    private final ClassLoader classLoader = null;
    @Alias
    private final boolean aggregate = false;

    @Substitute
    private ResourceBundle loadBundle(ClassLoader classLoader, Locale locale, String message) {
        ResourceBundle rb = null;
        try {
            if (aggregate) {
                rb = ResourceBundle.getBundle(
                        bundleName,
                        locale,
                        classLoader,
                        AggregateResourceBundle.CONTROL
                );
            } else {
                rb = ResourceBundle.getBundle(
                        bundleName,
                        locale,
                        classLoader
                );
            }
        } catch (Throwable e) {
            log.trace(message);
        }
        return rb;
    }


    private static class AggregateResourceBundleControl extends ResourceBundle.Control {
        @Override
        public ResourceBundle newBundle(
                String baseName,
                Locale locale,
                String format,
                ClassLoader loader,
                boolean reload)
                throws IllegalAccessException, InstantiationException, IOException {
            // only *.properties files can be aggregated. Other formats are delegated to the default implementation
            if (!"java.properties".equals(format)) {
                return super.newBundle(baseName, locale, format, loader, reload);
            }

            String resourceName = toBundleName(baseName, locale) + ".properties";
            Properties properties = load(resourceName, loader);
            return properties.size() == 0 ? null : new AggregateResourceBundle(properties);
        }

        private Properties load(String resourceName, ClassLoader loader) throws IOException {
            Properties aggregatedProperties = new Properties();
            Enumeration<URL> urls = GetResources.action(loader, resourceName).run();
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                Properties properties = new Properties();
                properties.load(url.openStream());
                aggregatedProperties.putAll(properties);
            }
            return aggregatedProperties;
        }
    }

    private static class AggregateResourceBundle extends ResourceBundle {

        protected static final Control CONTROL = new AggregateResourceBundleControl();
        private final Properties properties;

        protected AggregateResourceBundle(Properties properties) {
            this.properties = properties;
        }

        @Override
        protected Object handleGetObject(String key) {
            return properties.get(key);
        }

        @Override
        public Enumeration<String> getKeys() {
            Set<String> keySet = newHashSet();
            keySet.addAll(properties.stringPropertyNames());
            if (parent != null) {
                keySet.addAll(Collections.list(parent.getKeys()));
            }
            return Collections.enumeration(keySet);
        }
    }
}
