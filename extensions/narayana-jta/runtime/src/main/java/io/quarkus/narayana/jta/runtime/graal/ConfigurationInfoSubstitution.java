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

package io.quarkus.narayana.jta.runtime.graal;

import com.arjuna.common.util.ConfigurationInfo;
import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(ConfigurationInfo.class)
final class ConfigurationInfoSubstitution {

    @Alias
    private static String sourceId;
    @Alias
    private static String propertiesFile;
    @Alias
    private static String buildId;
    @Alias
    private static boolean isInitialized = false;

    // initialize build time properties from data in the jar's META-INF/MANIFEST.MF
    //TODO: actually implement this somehow so these values are baked in at build time
    @Substitute
    private static synchronized void getBuildTimeProperties() {
        if (isInitialized) {
            return;
        }
        propertiesFile = "jbossts-properties.xml";
        sourceId = "4d505";
        buildId = "JBoss Inc. [ochaloup] Linux 4.15.4-200.fc26.x86_64 2018/Feb/26 19:35";
        isInitialized = true;
    }
}
