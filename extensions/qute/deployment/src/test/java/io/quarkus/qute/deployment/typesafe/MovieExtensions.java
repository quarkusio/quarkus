package io.quarkus.qute.deployment.typesafe;

import io.quarkus.qute.TemplateExtension;

@TemplateExtension
public class MovieExtensions {

    static Long toNumber(Movie movie, String name) {
        return 43l;
    }

}
