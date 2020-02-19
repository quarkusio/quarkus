package io.quarkus.qute.runtime.extensions;

import io.quarkus.qute.TemplateExtension;

@TemplateExtension
public class NumberTemplateExtensions {

    static Integer mod(Integer number, Integer mod) {
        return number % mod;
    }

}