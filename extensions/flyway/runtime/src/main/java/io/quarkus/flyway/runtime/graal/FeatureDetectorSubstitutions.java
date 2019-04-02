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

package io.quarkus.flyway.runtime.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@Substitute
@TargetClass(className = "org.flywaydb.core.internal.util.FeatureDetector")
public final class FeatureDetectorSubstitutions {

    @Substitute
    public FeatureDetectorSubstitutions(ClassLoader classLoader) {

    }

    @Substitute
    public boolean isApacheCommonsLoggingAvailable() {
        return false;
    }

    @Substitute
    public boolean isSlf4jAvailable() {
        return false;
    }

    @Substitute
    public boolean isSpringJdbcAvailable() {
        return false;
    }

    @Substitute
    public boolean isJBossVFSv2Available() {
        return false;
    }

    @Substitute
    public boolean isJBossVFSv3Available() {
        return false;
    }

    @Substitute
    public boolean isOsgiFrameworkAvailable() {
        return false;
    }

    @Substitute
    public boolean isAndroidAvailable() {
        return false;
    }

}
