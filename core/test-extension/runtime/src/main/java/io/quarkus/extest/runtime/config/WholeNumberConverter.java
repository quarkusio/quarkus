package io.quarkus.extest.runtime.config;

import org.eclipse.microprofile.config.spi.Converter;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class WholeNumberConverter implements Converter<Integer> {

    public WholeNumberConverter() {
    }

    @Override
    public Integer convert(String s) {
        if (s == null || s.isEmpty()) {
            return 0;
        }

        switch (s) {
            case "zero":
                return 0;
            case "one":
                return 1;
            case "two":
                return 2;
            case "three":
                return 3;
            case "four":
                return 4;
            case "five":
                return 5;
            case "six":
                return 6;
            case "seven":
                return 7;
            case "eight":
                return 8;
            case "nine":
                return 9;
        }

        throw new IllegalArgumentException("Unsupported value " + s + "given");
    }
}
