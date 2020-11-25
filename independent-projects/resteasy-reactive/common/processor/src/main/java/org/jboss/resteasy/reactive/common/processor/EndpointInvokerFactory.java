package org.jboss.resteasy.reactive.common.processor;

import java.util.function.Supplier;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.resteasy.reactive.common.model.ResourceMethod;
import org.jboss.resteasy.reactive.spi.EndpointInvoker;

public interface EndpointInvokerFactory {

    public Supplier<EndpointInvoker> create(ResourceMethod method, ClassInfo currentClass, MethodInfo currentMethod);
}
