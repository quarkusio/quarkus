/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.renov8.resolver;

import org.jboss.renov8.Pack;
import org.jboss.renov8.PackLocation;
import org.jboss.renov8.PackVersion;
import org.jboss.renov8.Renov8Exception;

/**
 * Repository and application-specific  pack resolver which
 * return an implementation of Pack given its location.
 *
 * @author Alexey Loubyansky
 */
public interface PackResolver<P extends Pack> {

    /**
     * Resolves a pack given its location.
     *
     * @param location  pack location
     * @return  resolved pack
     * @throws Renov8Exception  in case of a failure
     */
    P resolve(PackLocation location) throws Renov8Exception;

    /**
     * Returns the latest available version of the pack given its location
     * @param location  pack location
     * @return  the latest available version
     * @throws Renov8Exception  in case of a failure
     */
    PackVersion getLatestVersion(PackLocation location) throws Renov8Exception;
}
