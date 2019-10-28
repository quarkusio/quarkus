package io.quarkus.vault.transit;

import static java.util.stream.Collectors.toMap;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import io.quarkus.vault.VaultException;
import io.quarkus.vault.runtime.transit.VaultTransitBatchResult;

/**
 * Base class for batch exceptions.
 */
public abstract class VaultBatchException extends VaultException {

    protected <K, V extends VaultTransitBatchResult, T> Map<K, T> filter(Map<K, V> results, Predicate<V> predicate,
            Function<V, T> function) {
        return results.entrySet().stream()
                .filter(entry -> predicate.test(entry.getValue()))
                .collect(toMap(Map.Entry::getKey, entry -> function.apply(entry.getValue())));
    }

    protected <K, V extends VaultTransitBatchResult> Map<K, String> getErrors(Map<K, V> results) {
        return filter(results, VaultTransitBatchResult::isInError, VaultTransitBatchResult::getError);
    }

    protected <K, V extends VaultTransitBatchResult, T> Map<K, T> getValid(Map<K, V> results) {
        return filter(results, VaultTransitBatchResult::isValid, VaultTransitBatchResult<T>::getValue);
    }

    public VaultBatchException(String message) {
        super(message);
    }

}
