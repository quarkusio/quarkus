package io.quarkus.qute.debug.agent.resolvers;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import io.quarkus.qute.debug.agent.RemoteStackFrame;

public interface ValueResolverContext {

    Object getBase();

    RemoteStackFrame getStackFrame();

    void addMethod(Method method);

    void addProperty(Field field);

    void addProperty(String property);

    void addMethod(String method);

    boolean isCollectProperty();

    boolean isCollectMethod();
}
