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

package org.jboss.builder;

/**
 * A single atomic unit of build work. A build step either succeeds or it does not, with no intermediate states
 * possible. Build steps should be as fine-grained as possible.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
@FunctionalInterface
public interface BuildStep {
    /**
     * Execute a build step.
     *
     * @param context the context of the build operation (not {@code null})
     */
    void execute(BuildContext context);

    /**
     * The empty build step, which immediately succeeds.
     */
    BuildStep EMPTY = context -> {
    };
}
