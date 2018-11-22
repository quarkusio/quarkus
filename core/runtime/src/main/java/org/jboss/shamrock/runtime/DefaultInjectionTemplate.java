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

package org.jboss.shamrock.runtime;

@Template
public class DefaultInjectionTemplate {

    public InjectionFactory defaultFactory() {
        return new DefaultInjectionFactory();
    }

    private static class DefaultInjectionFactory implements InjectionFactory {
        @Override
        public <T> InjectionInstance<T> create(Class<T> type) {
            return new InjectionInstance<T>() {

                @Override
                public T newInstance() {
                    try {
                        return type.newInstance();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            };
        }
    }
}
