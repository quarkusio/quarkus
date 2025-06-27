package io.quarkus.vertx.http.runtime.security;

import java.util.Optional;

import io.quarkus.vertx.http.security.event.Basic;

final class BasicImpl implements Basic {

    private Optional<Boolean> enabled;
    private String realm;
    private boolean built;

    BasicImpl(Optional<Boolean> enabled, String realm) {
        this.enabled = enabled;
        this.realm = realm;
        this.built = false;
    }

    @Override
    public Basic enable() {
        assertNotBuilt();
        this.enabled = Optional.of(true);
        return this;
    }

    @Override
    public Basic realm(String realm) {
        assertNotBuilt();
        this.realm = realm;
        return this;
    }

    BasicImpl build() {
        this.built = true;
        return this;
    }

    String getRealm() {
        return realm;
    }

    Optional<Boolean> getEnabled() {
        return enabled;
    }

    boolean isExplicitlyEnabled() {
        return enabled.orElse(false);
    }

    private void assertNotBuilt() {
        if (built) {
            throw new IllegalStateException("Basic authentication is already configured");
        }
    }
}
