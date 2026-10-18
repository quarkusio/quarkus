package io.quarkus.funqy.runtime.bindings.knative.events.filters;

import java.util.function.Predicate;

import io.quarkus.funqy.knative.events.CloudEvent;

public class CEAttributeLiteralEqualsFilter implements Predicate<CloudEvent> {

    private String attributeName;
    private String expectedValue;

    public CEAttributeLiteralEqualsFilter(String attributeName, String expectedValue) {
        this.attributeName = attributeName;
        this.expectedValue = expectedValue;
    }

    @Override
    public boolean test(CloudEvent event) {

        CloudEvent<?> ceEvent = (CloudEvent<?>) event;
        String value;

        switch (attributeName) {
            case "id":
                value = ceEvent.id();
                break;
            case "source":
                value = ceEvent.source();
                break;
            case "type":
                value = ceEvent.type();
                break;
            case "subject":
                value = ceEvent.subject();
                break;

            default:
                value = ceEvent.extensions().get(attributeName);
                break;
        }
        if (value == null) {
            return false;
        }

        return value.equals(expectedValue);
    }

    @Override
    public String toString() {
        return "[attributeName=" + attributeName + ", expectedValue='" + expectedValue + "']";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((attributeName == null) ? 0 : attributeName.hashCode());
        result = prime * result + ((expectedValue == null) ? 0 : expectedValue.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CEAttributeLiteralEqualsFilter other = (CEAttributeLiteralEqualsFilter) obj;
        if (attributeName == null) {
            if (other.attributeName != null)
                return false;
        } else if (!attributeName.equals(other.attributeName))
            return false;
        if (expectedValue == null) {
            if (other.expectedValue != null)
                return false;
        } else if (!expectedValue.equals(other.expectedValue))
            return false;
        return true;
    }

}
