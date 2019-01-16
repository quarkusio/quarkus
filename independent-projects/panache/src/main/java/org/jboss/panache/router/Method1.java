package org.jboss.panache.router;

@FunctionalInterface
public interface Method1<Target,P1> extends MethodFinder {
	Object method(Target target, P1 param1);
}
