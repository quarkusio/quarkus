package io.quarkus.it.spring.boot;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("cl")
public final class ClassProperties {

    private String value;

    private AnotherClass anotherClass;

    private Interface anInterface;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public AnotherClass getAnotherClass() {
        return anotherClass;
    }

    public void setAnotherClass(AnotherClass anotherClass) {
        this.anotherClass = anotherClass;
    }

    public Interface getAnInterface() {
        return anInterface;
    }

    public void setAnInterface(Interface anInterface) {
        this.anInterface = anInterface;
    }
}
