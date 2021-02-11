package io.quarkus.hibernate.validator.deployment;

import java.util.stream.Collectors;

import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

class SimpleMethodSignatureKey {

    private final String key;

    SimpleMethodSignatureKey(MethodInfo method) {
        // Notes:
        // - MethodInfo.toString() is not usable here because it includes the declaring class
        // - just parameters() for the second part would include annotations (see Type.toString())
        key = method.name() + method.parameters().stream()
                .map(Type::name)
                .map(DotName::toString)
                .collect(Collectors.joining(", ", "(", ")"));
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        SimpleMethodSignatureKey other = (SimpleMethodSignatureKey) obj;
        return key.equals(other.key);
    }

    @Override
    public String toString() {
        return key;
    }
}
