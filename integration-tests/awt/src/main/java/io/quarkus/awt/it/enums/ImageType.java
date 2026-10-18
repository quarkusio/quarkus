package io.quarkus.awt.it.enums;

/**
 * Convenience util to java.awt.image.BufferedImage constants.
 */
public enum ImageType {
    TYPE_INT_RGB(1, true),
    TYPE_INT_ARGB(2, true),
    TYPE_INT_ARGB_PRE(3, true),
    TYPE_INT_BGR(4, true),
    TYPE_3BYTE_BGR(5, true),
    TYPE_4BYTE_ABGR(6, true),
    TYPE_4BYTE_ABGR_PRE(7, true),
    TYPE_USHORT_565_RGB(8, true),
    TYPE_USHORT_555_RGB(9, true),
    TYPE_BYTE_GRAY(10, false),
    TYPE_USHORT_GRAY(11, false),
    TYPE_BYTE_BINARY(12, false),
    TYPE_BYTE_INDEXED(13, false);

    public final int code;
    public final boolean blurrable;

    ImageType(int code, boolean blurrable) {
        this.code = code;
        this.blurrable = blurrable;
    }
}
