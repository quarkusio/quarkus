package io.quarkus.extest.runtime.config;

import org.eclipse.microprofile.config.spi.Converter;

public class YesNoConverter implements Converter<Boolean> {

    public YesNoConverter() {
    }

    @Override
    public Boolean convert(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }

        switch (s) {
            case "YES":
                return true;
            case "NO":
                return false;
        }

        throw new IllegalArgumentException("Unsupported value " + s + " given");
    }
}
