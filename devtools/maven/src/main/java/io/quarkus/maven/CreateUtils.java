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
package io.quarkus.maven;

import org.apache.commons.lang3.StringUtils;

public final class CreateUtils {

    private CreateUtils() {
        //Not to be constructed
    }

    public static String getDerivedPath(String className) {
        String[] resourceClassName = StringUtils.splitByCharacterTypeCamelCase(
                className.substring(className.lastIndexOf(".") + 1));
        return "/" + resourceClassName[0].toLowerCase();
    }
}
