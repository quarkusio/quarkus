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

package io.quarkus.security;

import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

import org.wildfly.security.auth.server.SecurityIdentity;

import io.undertow.security.idm.Account;

/**
 * An Undertow account implementation that maps to the Elytron {@link SecurityIdentity}
 */
public class ElytronAccount implements Account {

    private final SecurityIdentity securityIdentity;

    public ElytronAccount(SecurityIdentity securityIdentity) {
        this.securityIdentity = securityIdentity;
    }

    @Override
    public Principal getPrincipal() {
        return securityIdentity.getPrincipal();
    }

    @Override
    public Set<String> getRoles() {
        Set<String> roles = new HashSet<>();
        for (String i : securityIdentity.getRoles()) {
            roles.add(i);
        }
        return roles;
    }
}
