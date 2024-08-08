package io.quarkus.maven.config.doc;

record ConfigSectionJavadoc(String title, String details) {

    static ConfigSectionJavadoc of(String javadoc) {
        if (javadoc == null || javadoc.isBlank()) {
            return new ConfigSectionJavadoc(null, null);
        }

        javadoc = javadoc.trim();
        int dotIndex = javadoc.indexOf(".");

        if (dotIndex == -1 || dotIndex == javadoc.length() - 1) {
            return new ConfigSectionJavadoc(javadoc, null);
        }

        return new ConfigSectionJavadoc(javadoc.substring(0, dotIndex).trim(), javadoc.substring(dotIndex).trim());
    }
}
