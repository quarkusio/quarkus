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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Function;

import io.quarkus.security.credential.Credential;
import io.quarkus.security.identity.SecurityIdentity;

public class QuarkusSecurityIdentity implements SecurityIdentity {

    private final Principal principal;
    private final Set<String> roles;
    private final Set<Credential> credentials;
    private final Map<String, Object> attributes;
    private final List<Function<Permission, CompletionStage<Boolean>>> permissionCheckers;

    private QuarkusSecurityIdentity(Builder builder) {
        this.principal = builder.principal;
        this.roles = Collections.unmodifiableSet(builder.roles);
        this.credentials = Collections.unmodifiableSet(builder.credentials);
        this.attributes = Collections.unmodifiableMap(builder.attributes);
        this.permissionCheckers = Collections.unmodifiableList(builder.permissionCheckers);
    }

    @Override
    public Principal getPrincipal() {
        return principal;
    }

    @Override
    public boolean isAnonymous() {
        return false;
    }

    @Override
    public Set<String> getRoles() {
        return roles;
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
        return null;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public CompletionStage<Boolean> checkPermission(Permission permission) {
        if (permissionCheckers.isEmpty()) {
            return CompletableFuture.completedFuture(true);
        }
        List<CompletableFuture<Boolean>> results = new ArrayList<>(permissionCheckers.size());
        for (Function<Permission, CompletionStage<Boolean>> checker : permissionCheckers) {
            CompletionStage<Boolean> res = checker.apply(permission);
            if (res != null) {
                results.add(res.toCompletableFuture());
            }
        }
        if (results.isEmpty()) {
            return CompletableFuture.completedFuture(true);
        }
        if (results.size() == 1) {
            return results.get(0);
        }
        CompletionStage<Boolean> ret = results.get(0);
        for (int i = 1; i < results.size(); ++i) {
            ret = ret.thenCombine(results.get(i), new BiFunction<Boolean, Boolean, Boolean>() {
                @Override
                public Boolean apply(Boolean aBoolean, Boolean aBoolean2) {
                    if (aBoolean == null) {
                        return aBoolean2;
                    }
                    if (aBoolean2 == null) {
                        return aBoolean;
                    }
                    return aBoolean || aBoolean2;
                }
            });
        }
        return ret;

    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        Principal principal;
        Set<String> roles = new HashSet<>();
        Set<Credential> credentials = new HashSet<>();
        Map<String, Object> attributes = new HashMap<>();
        List<Function<Permission, CompletionStage<Boolean>>> permissionCheckers = new ArrayList<>();
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
        public Builder addPermissionChecker(Function<Permission, CompletionStage<Boolean>> function) {
            if (built) {
                throw new IllegalStateException();
            }
            permissionCheckers.add(function);
            return this;
        }

        public QuarkusSecurityIdentity build() {
            built = true;
            return new QuarkusSecurityIdentity(this);
        }
    }
}
