package io.quarkus.qute.runtime.extensions;

import jakarta.enterprise.inject.Vetoed;

import io.quarkus.qute.TemplateExtension;

@Vetoed // Make sure no bean is created from this class
@TemplateExtension
public class NumberTemplateExtensions {

    static Integer mod(Integer number, Integer mod) {
        return number % mod;
    }

    // addition

    @TemplateExtension(matchNames = { "plus", "+" })
    static Integer addToInt(Integer number, String name, Integer other) {
        return number + other;
    }

    @TemplateExtension(matchNames = { "plus", "+" })
    static Long addToInt(Integer number, String name, Long other) {
        return number + other;
    }

    @TemplateExtension(matchNames = { "plus", "+" })
    static Long addToLong(Long number, String name, Integer other) {
        return number + other;
    }

    @TemplateExtension(matchNames = { "plus", "+" })
    static Long addToLong(Long number, String name, Long other) {
        return number + other;
    }

    // subtraction

    @TemplateExtension(matchNames = { "minus", "-" })
    static Integer subtractFromInt(Integer number, String name, Integer other) {
        return number - other;
    }

    @TemplateExtension(matchNames = { "minus", "-" })
    static Long subtractFromInt(Integer number, String name, Long other) {
        return number - other;
    }

    @TemplateExtension(matchNames = { "minus", "-" })
    static Long subtractFromLong(Long number, String name, Integer other) {
        return number - other;
    }

    @TemplateExtension(matchNames = { "minus", "-" })
    static Long subtractFromLong(Long number, String name, Long other) {
        return number - other;
    }

}
