package org.jboss.panache.router;

@FunctionalInterface
public interface Method0<Target> extends MethodFinder {
	Object method(Target target);
}
