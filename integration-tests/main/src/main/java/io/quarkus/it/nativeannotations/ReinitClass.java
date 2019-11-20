package io.quarkus.it.nativeannotations;

import io.quarkus.runtime.annotations.RuntimeReinitialized;

@RuntimeReinitialized
public class ReinitClass {

    static long timestamp = System.nanoTime();

}
