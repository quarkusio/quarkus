package org.jboss.panache.router;

@FunctionalInterface
public interface Method4<Target, P1, P2, P3, P4> extends MethodFinder {
	Object method(Target target, P1 param1, P2 param2, P3 param3, P4 param4);
}
