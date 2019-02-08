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

package org.jboss.shamrock.runtime.graal;

import java.util.logging.Handler;

import org.jboss.logmanager.handlers.DelayedHandler;
import org.jboss.shamrock.runtime.logging.InitialConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 */
@TargetClass(className = "org.jboss.logmanager.LoggerNode")
final class Target_org_jboss_logmanager_LoggerNode {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset)
    volatile Handler[] handlers;
}

@TargetClass(className = "org.slf4j.LoggerFactory")
final class Target_org_slf4j_LoggerFactory {

    @Substitute
    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz.getName());
    }
}

@TargetClass(InitialConfigurator.class)
final class Target_org_jboss_shamrock_runtime_logging_InitialConfigurator {
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    @Alias
    public static DelayedHandler DELAYED_HANDLER = new DelayedHandler();
}

final class LoggingSubstitutions {}
