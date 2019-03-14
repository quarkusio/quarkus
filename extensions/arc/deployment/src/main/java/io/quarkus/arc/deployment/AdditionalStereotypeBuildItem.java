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

package io.quarkus.arc.deployment;

import java.util.Collection;
import java.util.Map;

import org.jboss.builder.item.MultiBuildItem;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

/**
 * A map of additional stereotype classes to their instances that we want to process.
 */
public final class AdditionalStereotypeBuildItem extends MultiBuildItem {

    private final Map<DotName, Collection<AnnotationInstance>> stereotypes;

    public AdditionalStereotypeBuildItem(final Map<DotName, Collection<AnnotationInstance>> stereotypes) {
        this.stereotypes = stereotypes;
    }

    public Map<DotName, Collection<AnnotationInstance>> getStereotypes() {
        return stereotypes;
    }
}
