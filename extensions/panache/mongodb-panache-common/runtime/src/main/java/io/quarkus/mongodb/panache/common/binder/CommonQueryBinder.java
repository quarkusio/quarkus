package io.quarkus.mongodb.panache.common.binder;

final class CommonQueryBinder {

    private CommonQueryBinder() {
    }

    /**
     * Converts parameter values that need special handling before being passed to the MongoDB driver.
     * Enums are converted to their {@link Enum#name()} since the driver has no built-in enum codec.
     * All other values are returned as-is — the driver's CodecRegistry handles encoding.
     */
    static Object paramValue(Object value) {
        if (value != null && value.getClass().isEnum()) {
            return ((Enum<?>) value).name();
        }
        return value;
    }
}
