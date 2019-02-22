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

package io.quarkus.hibernate.orm.runtime;

import org.hibernate.cfg.AvailableSettings;
import org.jboss.logging.Logger;

public class Hibernate {

    static {
        // Override the JPA persistence unit resolver so to use our custom boot
        // strategy:
        PersistenceProviderSetup.registerPersistenceProvider();

        // We do our own enhancement during the compilation phase, so disable any
        // automatic entity enhancement by Hibernate ORM
        // This has to happen before Hibernate ORM classes are initialized: see
        // org.hibernate.cfg.Environment#BYTECODE_PROVIDER_INSTANCE
        System.setProperty(AvailableSettings.BYTECODE_PROVIDER,
                org.hibernate.cfg.Environment.BYTECODE_PROVIDER_NAME_NONE);
    }

    public static void featureInit() {
        Logger.getLogger("org.hibernate.quarkus.feature").debug("Hibernate Features Enabled");
    }

}
