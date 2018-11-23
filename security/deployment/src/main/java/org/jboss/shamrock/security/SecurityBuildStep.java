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

package org.jboss.shamrock.security;

import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.builder.item.SimpleBuildItem;
import org.jboss.shamrock.annotations.BuildProducer;
import org.jboss.shamrock.annotations.BuildStep;
import org.jboss.shamrock.annotations.ExecutionTime;
import org.jboss.shamrock.annotations.Record;
import org.jboss.shamrock.deployment.builditem.substrate.ReflectiveClassBuildItem;
import org.jboss.shamrock.runtime.RuntimeValue;
import org.jboss.shamrock.undertow.ServletExtensionBuildItem;
import org.wildfly.security.auth.realm.LegacyPropertiesSecurityRealm;
import org.wildfly.security.auth.server.SecurityDomain;

class SecurityBuildStep {


    @ConfigProperty(name = "shamrock.security.file")
    Optional<FileRealmConfig> fileRealmConfig;


    @BuildStep
    void services(BuildProducer<ReflectiveClassBuildItem> classes) {
        classes.produce(new ReflectiveClassBuildItem(false, false, "org.wildfly.security.password.impl.PasswordFactorySpiImpl"));
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    SecurityDomainBuildItem build(SecurityTemplate template, BuildProducer<ServletExtensionBuildItem> extension, LegacyPropertiesRealmBuildItem item) throws Exception {
        if (fileRealmConfig.isPresent()) {
            RuntimeValue<SecurityDomain> securityDomain = template.configureDomain(fileRealmConfig.get(), item.getRealm());
            extension.produce(new ServletExtensionBuildItem(template.configureUndertowIdentityManager(securityDomain)));
            return new SecurityDomainBuildItem(securityDomain);
        }
        return null;
    }


    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    LegacyPropertiesRealmBuildItem build(SecurityTemplate template) throws Exception {
        if (fileRealmConfig.isPresent()) {
            RuntimeValue<LegacyPropertiesSecurityRealm> realm = template.createRealm();
            return new LegacyPropertiesRealmBuildItem(realm);
        }
        return null;
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void loadRealm(SecurityTemplate template, LegacyPropertiesRealmBuildItem item) throws Exception {
        template.loadRealm(item.getRealm(), fileRealmConfig.get());
    }

    public static final class LegacyPropertiesRealmBuildItem extends SimpleBuildItem {
        private final RuntimeValue<LegacyPropertiesSecurityRealm> realm;

        public LegacyPropertiesRealmBuildItem(RuntimeValue<LegacyPropertiesSecurityRealm> realm) {
            this.realm = realm;
        }

        public RuntimeValue<LegacyPropertiesSecurityRealm> getRealm() {
            return realm;
        }
    }
}
