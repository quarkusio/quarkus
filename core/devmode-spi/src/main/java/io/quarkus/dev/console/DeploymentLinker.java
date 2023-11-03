package io.quarkus.dev.console;

import java.util.Map;

/**
 * Creates "links" to objects between deployment and runtime,
 * essentially exposing the same interface but on a different classloader.
 * <p>
 * This implies all communication must go through JDK classes, so the transfer involves Maps, Functions, ...
 * Yes this is awful. No there's no better solution ATM.
 * Ideally we'd automate this through bytecode generation,
 * but feasibility is uncertain, and we'd need a volunteer who has time for that.
 * <p>
 * Implementations should live in the runtime module.
 * To transfer {@link #createLinkData(Object) link data} between deployment and runtime,
 * see {@link DevConsoleManager#setGlobal(String, Object)} and {@link DevConsoleManager#getGlobal(String)}.
 */
public interface DeploymentLinker<T> {

    /**
     * @param object An object implementing class {@code T} in either the current classloader.
     * @return A classloader-independent map containing Functions, Suppliers, etc.
     *         giving access to the object's methods,
     *         which will be passed to {@link #createLink(Map)} from the other classloader.
     */
    Map<String, ?> createLinkData(T object);

    /**
     * @param linkData The result of calling {@link #createLinkData(Object)}.
     * @return An object implementing class {@code T} in the current classloader
     *         and redirecting calls to the Functions, Suppliers, etc. from {@code linkData},
     *         thereby linking to the implementation in its original classloader.
     */
    T createLink(Map<String, ?> linkData);

}
