# Resteasy Reactive Testuite Wrapper

This Maven module wraps the execution of https://github.com/quarkusio/resteasy-reactive-testsuite
to leverage incremental build mechanics and to make it easier to run locally.

That repo will be cloned and executed via a separate mvn call when running `mvn test`.

The Git ref is pinned in `pom.xml` and can be changed conveniently via `./update-ref.sh <new-ref>`.

If dependencies were changed in the testuite repo make sure to run `./update-dependencies.sh`
to ensure proper build order.

`mvn clean ...` will remove the cloned repo so better use `mvn test -Dresteasy-reactive-testsuite.clone.skip`
to save some time in case you want to run the suite multiple times.

More properties:
- `resteasy-reactive-testsuite.checkout.skip` skips (re-)checking out the respective ref
- `resteasy-reactive-testsuite.test.skip` skips the execution of the suite
- `resteasy-reactive-testsuite.repo.org` can be used to target a fork
- `resteasy-reactive-testsuite.repo.ref` defines the ref to check out
