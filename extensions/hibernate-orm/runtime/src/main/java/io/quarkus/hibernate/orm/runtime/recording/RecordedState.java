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

package io.quarkus.hibernate.orm.runtime.recording;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;

import io.quarkus.hibernate.orm.runtime.BuildTimeSettings;

public final class RecordedState {

    private final Dialect dialect;
    private final MetadataImplementor metadata;
    private final JtaPlatform jtaPlatform;
    private final BuildTimeSettings settings;

    public RecordedState(Dialect dialect, JtaPlatform jtaPlatform, MetadataImplementor metadata,
            BuildTimeSettings settings) {
        this.dialect = dialect;
        this.jtaPlatform = jtaPlatform;
        this.metadata = metadata;
        this.settings = settings;
    }

    public Dialect getDialect() {
        return dialect;
    }

    public MetadataImplementor getMetadata() {
        return metadata;
    }

    public BuildTimeSettings getBuildTimeSettings() {
        return settings;
    }

    public JtaPlatform getJtaPlatform() {
        return jtaPlatform;
    }
}
