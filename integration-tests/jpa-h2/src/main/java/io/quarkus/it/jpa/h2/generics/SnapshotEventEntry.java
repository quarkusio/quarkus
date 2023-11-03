package io.quarkus.it.jpa.h2.generics;

import jakarta.persistence.Entity;

/**
 * This strange mapping is useful to verify that Hibernate ORM can
 * actually boot, which implies to successfully narrow down the T
 * generics of the two superclass types.
 * See also HHH-14499 for details; the reason this directly impacts
 * Quarkus is that the issue can be triggered by enlisting the
 * parent classes (mapped with MappedSuperclass) among the discovered
 * entities - which is not necessary and actually triggers the problem.
 * So this is a regression test to check that Quarkus is NOT passing
 * such abstract types as mapped entities to the ORM bootstrap.
 * No actual test is necessary: Hibernate ORM will fail to boot
 * (likely but not guaranteed as this is dependent on the order
 * of entity discovery) simply because these entities exist on the
 * same classpath.
 */
@Entity
public class SnapshotEventEntry extends IntermediateAbstractMapped<byte[]> {

}
