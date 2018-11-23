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

import org.wildfly.security.auth.server.RealmUnavailableException;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.evidence.PasswordGuessEvidence;

import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.idm.PasswordCredential;

public class ElytronIdentityManager implements IdentityManager {

    private final SecurityDomain domain;

    public ElytronIdentityManager(SecurityDomain domain) {
        this.domain = domain;
    }

    @Override
    public Account verify(Account account) {
        return account;
    }

    @Override
    public Account verify(String id, Credential credential) {
        try {
            if (credential instanceof PasswordCredential) {
                PasswordCredential passwordCredential = (PasswordCredential) credential;
                try {
                    SecurityIdentity result = domain.authenticate(id, new PasswordGuessEvidence(passwordCredential.getPassword()));
                    if (result != null) {
                        return new ElytronAccount(result);
                    }
                } catch (RealmUnavailableException e) {
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Account verify(Credential credential) {
        return null;
    }
}
