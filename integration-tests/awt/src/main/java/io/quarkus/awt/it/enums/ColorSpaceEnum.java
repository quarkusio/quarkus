package io.quarkus.awt.it.enums;

/**
 * Convenience util to java.awt.color.ColorSpace constants.
 */
public enum ColorSpaceEnum {
    CS_sRGB(1000),
    CS_LINEAR_RGB(1004),
    CS_CIEXYZ(1001),
    CS_PYCC(1002),
    CS_GRAY(1003),
    CS_DEFAULT(-1);

    public final int code;

    ColorSpaceEnum(int code) {
        this.code = code;
    }
}
