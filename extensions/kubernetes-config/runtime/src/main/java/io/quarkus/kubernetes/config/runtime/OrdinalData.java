package io.quarkus.kubernetes.config.runtime;

enum OrdinalData {

    CONFIG_MAP(
            270, // this is higher than the file system or jar ordinals, but lower than env vars
            284 // this is one less than the ordinal of Secret
    ),

    SECRET(
            285, // this is one less than the ordinal of ConfigMap
            299 // this is one less than env vars
    );

    private final int base;
    private final int max;

    OrdinalData(int base, int max) {
        this.base = base;
        this.max = max;
    }

    public int getBase() {
        return base;
    }

    public int getMax() {
        return max;
    }
}
