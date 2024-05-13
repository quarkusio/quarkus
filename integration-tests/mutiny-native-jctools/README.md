# Quarkus - Integration Tests - Mutiny native JCTools support

This integration test checks that the Mutiny extension provides support for the native compilation of JCTools, which is now used internally in Mutiny instead of old custom data structures.

This is important as JCTools makes use of `sun.misc.Unsafe` in some places.

The tests do the following:

- create all kinds of queues behind the factory `io.smallrye.mutiny.helpers.queues.Queues` interface, and
- expose a few Mutiny pipelines where queues may be needed: overflow, custom emitters, etc.
