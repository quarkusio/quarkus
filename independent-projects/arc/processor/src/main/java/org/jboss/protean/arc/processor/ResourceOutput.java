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

package org.jboss.protean.arc.processor;

import java.io.File;
import java.io.IOException;

/**
 * Represents a generated resource.
 *
 * @author Martin Kouba
 */
public interface ResourceOutput {

    void writeResource(Resource resource) throws IOException;

    interface Resource {

        boolean isApplicationClass();

        File writeTo(File directory) throws IOException;

        byte[] getData();

        /**
         * <pre>
         * com/foo/MyBean
         * com/foo/MyBean$Bar
         * org.jboss.protean.arc.BeanProvider
         * </pre>
         *
         * @return the name
         */
        String getName();

        /**
         * <pre>
         * com.foo.MyBean
         * </pre>
         *
         * @return the fully qualified name as defined in JLS
         */
        default String getFullyQualifiedName() {
            if (Type.JAVA_CLASS.equals(getType()) || Type.JAVA_SOURCE.equals(getType())) {
                return getName().replace('/', '.');
            }
            throw new UnsupportedOperationException();
        }

        /**
         *
         * @see Type
         * @return the type
         */
        Type getType();

        /**
         *
         * @see SpecialType
         * @return the special type or null
         */
        SpecialType getSpecialType();

        enum Type {
            JAVA_CLASS, JAVA_SOURCE, SERVICE_PROVIDER,
        }

        enum SpecialType {
            BEAN, INTERCEPTOR_BEAN, OBSERVER;
        }

    }

}
