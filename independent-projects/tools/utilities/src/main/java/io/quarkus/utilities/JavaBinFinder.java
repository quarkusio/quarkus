package io.quarkus.utilities;

import io.smallrye.common.process.ProcessUtil;

/**
 * @deprecated Use {@link ProcessUtil} instead.
 */
@Deprecated(forRemoval = true, since = "3.25")
public class JavaBinFinder {
    /**
     * {@return the path of the {@code java} command (not {@code null})}
     *
     * @deprecated Use {@link ProcessUtil#pathOfJava()} instead.
     */
    @Deprecated(forRemoval = true, since = "3.25")
    public static String findBin() {
        return ProcessUtil.pathOfJava().toString();
    }

    /**
     * {@return the name of the {@code java} executable for this platform (not {@code null})}
     *
     * @deprecated Use {@link ProcessUtil#nameOfJava()} instead.
     */
    @Deprecated(forRemoval = true, since = "3.25")
    public static String simpleBinaryName() {
        return ProcessUtil.nameOfJava();
    }

}
