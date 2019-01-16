package org.jboss.panache.router;

@FunctionalInterface
public interface Method2<Target, P1, P2> extends MethodFinder {
	Object method(Target target, P1 param1, P2 param2);
}
