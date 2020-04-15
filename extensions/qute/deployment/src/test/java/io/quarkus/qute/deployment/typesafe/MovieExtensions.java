package io.quarkus.qute.deployment.typesafe;

import io.quarkus.qute.TemplateExtension;

@TemplateExtension
public class MovieExtensions {

    static Long toNumber(Movie movie, String name) {
        return 43l;
    }

    static Long toLong(Movie movie, long... values) {
        long ret = 0l;
        for (long value : values) {
            ret += value;
        }
        return ret;
    }

}
