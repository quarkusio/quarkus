package io.quarkus.test.component.declarative;

import jakarta.inject.Inject;

import io.quarkus.test.InjectMock;
import io.quarkus.test.component.beans.Charlie;
import io.quarkus.test.component.beans.MyComponent;

public abstract class SuperTest {

    @Inject
    MyComponent myComponent;

    @InjectMock
    Charlie charlie;

}
