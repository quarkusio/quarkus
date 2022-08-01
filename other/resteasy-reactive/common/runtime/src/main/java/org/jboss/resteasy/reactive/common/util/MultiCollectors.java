package org.jboss.resteasy.reactive.common.util;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import java.util.stream.Collectors;

/**
 * Set of convenient collectors for dealing with {@link Multi} return types.
 */
// I've avoided creating collectors for silly things like Byte and Character because
// they can't possibly be useful, and even the JAX-RS spec doesn't mandate we have writers
// for them
public class MultiCollectors {

    public static Uni<String> concatenateStrings(Multi<String> multi) {
        return multi.collect().with(Collectors.joining());
    }

    public static Uni<byte[]> concatenateByteArrays(Multi<byte[]> multi) {
        // we could avoid the list and grow an array as we collect, but I doubt that's more efficient TBH
        return multi.collect().asList()
                .map(list -> {
                    int size = 0;
                    for (byte[] array : list) {
                        size += array.length;
                    }
                    byte[] ret = new byte[size];
                    int i = 0;
                    for (byte[] array : list) {
                        System.arraycopy(array, 0, ret, i, array.length);
                        i += array.length;
                    }
                    return ret;
                });
    }

    public static Uni<char[]> concatenateCharArrays(Multi<char[]> multi) {
        // we could avoid the list and grow an array as we collect, but I doubt that's more efficient TBH
        return multi.collect().asList()
                .map(list -> {
                    int size = 0;
                    for (char[] array : list) {
                        size += array.length;
                    }
                    char[] ret = new char[size];
                    int i = 0;
                    for (char[] array : list) {
                        System.arraycopy(array, 0, ret, i, array.length);
                        i += array.length;
                    }
                    return ret;
                });
    }
}
