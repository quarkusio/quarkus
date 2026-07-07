package org.acme.application;

import org.acme.library.LibraryValue;

public final class ApplicationValue {

    private ApplicationValue() {
    }

    public static String value() {
        return LibraryValue.value();
    }
}
