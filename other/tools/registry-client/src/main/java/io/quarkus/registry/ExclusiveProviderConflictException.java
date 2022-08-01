package io.quarkus.registry;

import java.util.List;

@SuppressWarnings("serial")
class ExclusiveProviderConflictException extends Exception {

    List<RegistryExtensionResolver> conflictingRegistries;

    ExclusiveProviderConflictException(List<RegistryExtensionResolver> conflictingRegistries) {
        this.conflictingRegistries = conflictingRegistries;
    }
}
