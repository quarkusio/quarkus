package io.quarkus.it.panache.next;

/**
 * A non-Panache interface used as a parent for an inner interface
 * on a PanacheEntity. This triggers a bug in the Hibernate Processor
 * where the generated CDI accessor in the metamodel class uses
 * the unqualified inner interface name without importing it.
 *
 * @see EntityWithBareInnerInterface
 */
public interface CustomQueries {
}
