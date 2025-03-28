package io.quarkus.deployment.dev.devservices;

public record ImageName(String fullName, String registry, String repository, Version version) {

    private static final String LIBRARY_PREFIX = "library/";

    public static ImageName parse(String fullName) {
        final int slashIndex = fullName.indexOf('/');

        String remoteName;
        String registry;
        String repository;
        Version version;

        if (slashIndex == -1 ||
                (!fullName.substring(0, slashIndex).contains(".") &&
                        !fullName.substring(0, slashIndex).contains(":") &&
                        !fullName.substring(0, slashIndex).equals("localhost"))) {
            registry = "";
            remoteName = fullName;
        } else {
            registry = fullName.substring(0, slashIndex);
            remoteName = fullName.substring(slashIndex + 1);
        }

        if (remoteName.contains("@sha256:")) {
            repository = remoteName.split("@sha256:")[0];
            version = new Version.Sha256(remoteName.split("@sha256:")[1]);
        } else if (remoteName.contains(":")) {
            repository = remoteName.split(":")[0];
            version = new Version.Tag(remoteName.split(":")[1]);
        } else {
            repository = remoteName;
            version = new Version.Any();
        }
        return new ImageName(fullName, registry, repository, version);
    }

    /**
     * @return the unversioned (non 'tag') part of this name
     */
    public String getUnversionedPart() {
        if (!"".equals(registry)) {
            return registry + "/" + repository;
        } else {
            return repository;
        }
    }

    /**
     * @return the versioned part of this name (tag or sha256)
     */
    public String getVersionPart() {
        return version.toString();
    }

    public ImageName withLibraryPrefix() {
        if (repository.startsWith(LIBRARY_PREFIX)) {
            return this;
        }
        return new ImageName(fullName, registry, LIBRARY_PREFIX + repository, version);
    }

    @Override
    public String toString() {
        return getUnversionedPart() + version.getSeparator() + getVersionPart();
    }

    sealed static class Version permits Version.Tag, Version.Sha256, Version.Any {
        public final String version;

        public Version(String version) {
            this.version = version;
        }

        public String getVersion() {
            return version;
        }

        public String getSeparator() {
            return ":";
        }

        @Override
        public String toString() {
            return version + getSeparator();
        }

        private static final class Tag extends Version {

            public Tag(String version) {
                super(version);
            }
        }

        private static final class Sha256 extends Version {

            private Sha256(String version) {
                super(version);
            }

            @Override
            public String getSeparator() {
                return "@";
            }

            @Override
            public String toString() {
                return "sha256:" + version;
            }
        }

        private static final class Any extends Version {
            public Any() {
                super("latest");
            }
        }
    }
}
