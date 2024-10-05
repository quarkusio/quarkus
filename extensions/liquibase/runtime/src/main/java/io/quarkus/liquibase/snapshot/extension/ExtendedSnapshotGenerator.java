package io.quarkus.liquibase.snapshot.extension;

public interface ExtendedSnapshotGenerator<T, U> {

    U snapshot(T object);

    boolean supports(T object);

}
