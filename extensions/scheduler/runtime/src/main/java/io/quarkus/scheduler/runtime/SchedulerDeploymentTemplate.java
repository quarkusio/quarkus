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
package io.quarkus.scheduler.runtime;

import java.util.List;
import java.util.Map;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Template;
import io.quarkus.scheduler.api.Scheduled;

/**
 *
 * @author Martin Kouba
 */
@Template
public class SchedulerDeploymentTemplate {

    public static final String SCHEDULES_KEY = "schedules";
    public static final String INVOKER_KEY = "invoker";
    public static final String DESC_KEY = "desc";

    @SuppressWarnings("unchecked")
    public void registerSchedules(List<Map<String, Object>> configurations, BeanContainer container) {
        SchedulerConfiguration schedulerConfig = container.instance(SchedulerConfiguration.class);
        for (Map<String, Object> config : configurations) {
            schedulerConfig.register(config.get(INVOKER_KEY).toString(), config.get(DESC_KEY).toString(),
                    (List<Scheduled>) config.get(SCHEDULES_KEY));
        }
    }

}
