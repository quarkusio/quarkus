package org.jboss.shamrock.security.test;

import java.util.Map;

import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.AuthenticationMechanismFactory;
import io.undertow.security.idm.IdentityManager;
import io.undertow.server.handlers.form.FormParserFactory;

public class CustomAuthFactory implements AuthenticationMechanismFactory {
    @Override
    public AuthenticationMechanism create(String mechanismName, IdentityManager identityManager, FormParserFactory formParserFactory, Map<String, String> properties) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        return new CustomAuth();
    }

}
