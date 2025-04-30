package io.quarkus.hibernate.search.standalone.elasticsearch.runtime;

import java.util.Map;

/**
 * A class responsible for holding pre-boot state.
 * <p>
 * This is just a way to circumvent a mismatch between build-time order and static-init order:
 * <ol>
 * <li>At build time, the build step that records the static-init Hibernate Search pre-boot
 * must execute before build step that defines CDI beans,
 * which itself must execute before the Arc build step that records the CDI container initialization.
 * <li>During static init, the CDI container initialization must happen before
 * the Hibernate Search pre-boot.
 * </ol
 * Because of this mismatch,
 * we can't rely on passing around the values returned by bytecode recorder between build steps.
 */
public final class HibernateSearchStandalonePreBootState {

    private static volatile Map<String, Object> state;

    /**
     * Sets the pre-boot state during static init.
     *
     * @param aState The pre-boot state as generated during static init.
     */
    public static void set(Map<String, Object> aState) {
        state = aState;
    }

    /**
     * Returns and clears the pre-boot state.
     *
     * @return The pre-boot state as generated during static init.
     */
    public static Map<String, Object> pop() {
        try {
            return state;
        } finally {
            state = null;
        }
    }

}
