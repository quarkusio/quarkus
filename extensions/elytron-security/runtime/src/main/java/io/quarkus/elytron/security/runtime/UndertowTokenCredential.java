package io.quarkus.elytron.security.runtime;

import io.undertow.security.idm.Credential;

public interface UndertowTokenCredential extends Credential {

    String getBearerToken();
}
