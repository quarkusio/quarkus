package io.quarkus.security.runtime;

import java.security.Permission;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import io.quarkus.security.credential.Credential;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;

public class QuarkusSecurityIdentity implements SecurityIdentity {

    private final Principal principal;
    private final Set<String> roles;
    private final Set<Credential> credentials;
    private final Map<String, Object> attributes;
    private final List<Function<Permission, Uni<Boolean>>> permissionCheckers;
    private final boolean anonymous;

    private QuarkusSecurityIdentity(Builder builder) {
        this.principal = builder.principal;
        this.roles = Collections.unmodifiableSet(builder.roles);
        this.credentials = Collections.unmodifiableSet(builder.credentials);
        this.attributes = Collections.unmodifiableMap(builder.attributes);
        this.permissionCheckers = Collections.unmodifiableList(builder.permissionCheckers);
        this.anonymous = builder.anonymous;
    }

    @Override
    public Principal getPrincipal() {
        return principal;
    }

    @Override
    public boolean isAnonymous() {
        return anonymous;
    }

    @Override
    public Set<String> getRoles() {
        return roles;
    }

    @Override
    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    @Override
    public <T extends Credential> T getCredential(Class<T> credentialType) {
        for (Credential i : credentials) {
            if (credentialType.isAssignableFrom(i.getClass())) {
                return (T) i;
            }
        }
        return null;
    }

    @Override
    public Set<Credential> getCredentials() {
        return credentials;
    }

    @Override
    public <T> T getAttribute(String name) {
        return (T) attributes.get(name);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Uni<Boolean> checkPermission(Permission permission) {
        if (permissionCheckers.isEmpty()) {
            return Uni.createFrom().item(true);
        }
        List<Uni<Boolean>> results = new ArrayList<>(permissionCheckers.size());
        for (Function<Permission, Uni<Boolean>> checker : permissionCheckers) {
            Uni<Boolean> res = checker.apply(permission);
            if (res != null) {
                results.add(res);
            }
        }
        if (results.isEmpty()) {
            return Uni.createFrom().item(true);
        }
        if (results.size() == 1) {
            return results.get(0);
        }
        return Uni.combine().all().unis(results).combinedWith(new Function<List<?>, Boolean>() {
            @Override
            public Boolean apply(List<?> o) {
                Boolean result = null;
                //if any are true we return true
                //otherwise if all are null we return null
                //if some are false and some null we return false
                for (Object i : o) {
                    if (i != null) {
                        boolean val = (boolean) i;
                        if (val) {
                            return true;
                        }
                        result = false;
                    }
                }
                return result;
            }
        });

    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(SecurityIdentity identity) {
        Builder builder = new Builder()
                .addAttributes(identity.getAttributes())
                .addCredentials(identity.getCredentials())
                .addRoles(identity.getRoles())
                .setPrincipal(identity.getPrincipal())
                .setAnonymous(identity.isAnonymous());
        return builder;
    }

    public static class Builder {

        Principal principal;
        Set<String> roles = new HashSet<>();
        Set<Credential> credentials = new HashSet<>();
        Map<String, Object> attributes = new HashMap<>();
        List<Function<Permission, Uni<Boolean>>> permissionCheckers = new ArrayList<>();
        private boolean anonymous;
        boolean built = false;

        public Builder setPrincipal(Principal principal) {
            if (built) {
                throw new IllegalStateException();
            }
            this.principal = principal;
            return this;
        }

        public Builder addRole(String role) {
            if (built) {
                throw new IllegalStateException();
            }
            this.roles.add(role);
            return this;
        }

        public Builder addRoles(Set<String> roles) {
            if (built) {
                throw new IllegalStateException();
            }
            this.roles.addAll(roles);
            return this;
        }

        public Builder addCredential(Credential credential) {
            if (built) {
                throw new IllegalStateException();
            }
            credentials.add(credential);
            return this;
        }

        public Builder addCredentials(Set<Credential> credentials) {
            if (built) {
                throw new IllegalStateException();
            }
            this.credentials.addAll(credentials);
            return this;
        }

        public Builder addAttribute(String key, Object value) {
            if (built) {
                throw new IllegalStateException();
            }
            attributes.put(key, value);
            return this;
        }

        public Builder addAttributes(Map<String, Object> attributes) {
            if (built) {
                throw new IllegalStateException();
            }
            this.attributes.putAll(attributes);
            return this;
        }

        /**
         * Adds a permission checker function. This permission checker has the following semantics:
         *
         * If it returns null, or the CompletionStage evaluates to null then this check is ignored
         * If every function returns null or false then the check is failed
         * If any function returns true the check passes
         *
         * @param function The permission checker function
         * @return This builder
         */
        public Builder addPermissionChecker(Function<Permission, Uni<Boolean>> function) {
            if (built) {
                throw new IllegalStateException();
            }
            permissionCheckers.add(function);
            return this;
        }

        /**
         * Sets an anonymous identity status.
         * 
         * @param anonymous the anonymous status
         * @return This builder
         */
        public Builder setAnonymous(boolean anonymous) {
            if (built) {
                throw new IllegalStateException();
            }
            this.anonymous = anonymous;
            return this;
        }

        public QuarkusSecurityIdentity build() {
            if (principal == null && !anonymous) {
                throw new IllegalStateException("Principal is null but anonymous status is false");
            }

            built = true;
            return new QuarkusSecurityIdentity(this);
        }
    }
}
