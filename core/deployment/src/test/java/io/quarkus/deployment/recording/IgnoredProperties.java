package io.quarkus.deployment.recording;

import java.util.Objects;

import io.quarkus.runtime.annotations.IgnoreProperty;

public class IgnoredProperties {

    private String notIgnored;
    @IgnoreProperty
    private String ignoredField;

    private transient String anotherIgnoredField;

    @TestRecordingAnnotationsProvider.TestIgnoreProperty
    private String customIgnoredField;

    public IgnoredProperties() {
    }

    public IgnoredProperties(String notIgnored, String ignoredField, String anotherIgnoredField, String customIgnoredField) {
        this.notIgnored = notIgnored;
        this.ignoredField = ignoredField;
        this.anotherIgnoredField = anotherIgnoredField;
        this.customIgnoredField = customIgnoredField;
    }

    public String getNotIgnored() {
        return notIgnored;
    }

    public void setNotIgnored(String notIgnored) {
        this.notIgnored = notIgnored;
    }

    public String getIgnoredField() {
        return ignoredField;
    }

    public void setIgnoredField(String ignoredField) {
        this.ignoredField = ignoredField;
    }

    public String getAnotherIgnoredField() {
        return anotherIgnoredField;
    }

    public void setAnotherIgnoredField(String anotherIgnoredField) {
        this.anotherIgnoredField = anotherIgnoredField;
    }

    public String getCustomIgnoredField() {
        return customIgnoredField;
    }

    public void setCustomIgnoredField(String customIgnoredField) {
        this.customIgnoredField = customIgnoredField;
    }

    @IgnoreProperty
    public String getSomethingElse() {
        throw new IllegalStateException("This should not have been called");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        IgnoredProperties that = (IgnoredProperties) o;
        return Objects.equals(notIgnored, that.notIgnored)
                && Objects.equals(ignoredField, that.ignoredField)
                && Objects.equals(anotherIgnoredField, that.anotherIgnoredField)
                && Objects.equals(customIgnoredField, that.customIgnoredField);
    }

    @Override
    public int hashCode() {
        return Objects.hash(notIgnored, ignoredField, anotherIgnoredField, customIgnoredField);
    }

    @Override
    public String toString() {
        return "IgnoredProperties{"
                + "notIgnored='" + notIgnored + '\''
                + ", ignoredField='" + ignoredField + '\''
                + ", anotherIgnoredField='" + anotherIgnoredField + '\''
                + ", customIgnoredField='" + customIgnoredField + '\''
                + '}';
    }
}
