package io.quarkus.analytics.dto.segment;

import java.io.Serializable;
import java.util.List;

public class TrackProperties implements Serializable {
    private List<AppExtension> appExtensions;

    public TrackProperties() {
    }

    public TrackProperties(List<AppExtension> appExtensions) {
        this.appExtensions = appExtensions;
    }

    public static TrackPropertiesBuilder builder() {
        return new TrackPropertiesBuilder();
    }

    public List<AppExtension> getAppExtensions() {
        return appExtensions;
    }

    public void setAppExtensions(List<AppExtension> appExtensions) {
        this.appExtensions = appExtensions;
    }

    public static class TrackPropertiesBuilder {
        private List<AppExtension> appExtensions;

        TrackPropertiesBuilder() {
        }

        public TrackPropertiesBuilder appExtensions(List<AppExtension> appExtensions) {
            this.appExtensions = appExtensions;
            return this;
        }

        public TrackProperties build() {
            return new TrackProperties(appExtensions);
        }

        public String toString() {
            return "TrackProperty.TrackPropertyBuilder(appExtensions=" + this.appExtensions + ")";
        }
    }

    public static class AppExtension {
        private String groupId;
        private String artifactId;
        private String version;

        public AppExtension() {
        }

        public AppExtension(String groupId, String artifactId, String version) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
        }

        public static AppExtensionBuilder builder() {
            return new AppExtensionBuilder();
        }

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public void setArtifactId(String artifactId) {
            this.artifactId = artifactId;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public static class AppExtensionBuilder {
            private String groupId;
            private String artifactId;
            private String version;

            AppExtensionBuilder() {
            }

            public AppExtensionBuilder groupId(String groupId) {
                this.groupId = groupId;
                return this;
            }

            public AppExtensionBuilder artifactId(String artifactId) {
                this.artifactId = artifactId;
                return this;
            }

            public AppExtensionBuilder version(String version) {
                this.version = version;
                return this;
            }

            public AppExtension build() {
                return new AppExtension(groupId, artifactId, version);
            }

            public String toString() {
                return "TrackProperty.AppExtension.AppExtensionBuilder(groupId=" + this.groupId +
                        ", artifactId=" + this.artifactId + ", version=" + this.version + ")";
            }
        }
    }
}
