# Jakarta migration

This directory contains scripts and configuration files to automate the migration to Jakarta EE 9 (for now) and hopefully Jakarta EE 10 soon.

## jakarta/transform.sh

`jakarta/transform.sh` is the main script to run.
It has to be run from the root of the Quarkus repository.

If you are offline and can't fetch the external projects, do:
```
export REWRITE_OFFLINE=true
jakarta/transform.sh
```
Obviously, you need to have run a full build before.
Also keep in mind you should run a full build from time to time to get the potential updates to OpenRewrite.

It consists of several steps that leverage various tools:

- [OpenRewrite](https://github.com/openrewrite/rewrite) - rewrites the POM files
- [Eclipse Transformer](https://projects.eclipse.org/projects/technology.transformer) - rewrites the classes

Note that this script also builds:

- A patched version of OpenRewrite (while waiting for integration of our patches)
- A patched version of the Rewrite Maven Plugin (to point to the patched OpenRewrite version)
- A patched version of the Kotlin Maven Plugin (to allow skipping the `main` compilation, it's required for the OpenRewrite run)

## Approach

### OpenRewrite

The OpenRewrite transformation is done in one unique step to alleviate dependency issues we had before switching to this approach.
You don't need to run OpenRewrite for the modules you add to `transform.sh`, you just need to adjust the configuration.

OpenRewrite recipes are declared in the `jakarta/rewrite.yml` file.

Documentation for writing recipes can be found:

- In the [official documentation](https://docs.openrewrite.org/reference/recipes/maven)
- In the [source for those not already released](https://github.com/gsmet/rewrite/tree/main/rewrite-maven/src/main/java/org/openrewrite/maven) (things are very simple there and well documented in the source)

Recipes are not applied on all POM files, you need to declare the ones you want to apply to a given target POM when changes are necessary.
Note that in most cases, you don't need to apply any changes.

The active recipes have to be declared in the target POM with a `plugin` block in the `<build><plugins>` section similar to:

```xml
            <plugin>
                <groupId>org.openrewrite.maven</groupId>
                <artifactId>rewrite-maven-plugin</artifactId>
                <configuration>
                    <activeRecipes>
                        <recipe>io.quarkus.maven.javax.managed</recipe>
                        <recipe>io.quarkus.maven.javax.versions</recipe>
                        <recipe>io.quarkus.jakarta-versions</recipe>
                    </activeRecipes>
                </configuration>
            </plugin>
```

If you declare the `rewrite-maven-plugin` in a POM that is a parent POM, make sure the plugin is not inherited by adding `<inherited>false</inherited>` to the `<plugin>` block
as it's highly probable you don't want to apply the same rule to the child POM files:

```xml
            <plugin>
                <inherited>false</inherited>
                <groupId>org.openrewrite.maven</groupId>
                <artifactId>rewrite-maven-plugin</artifactId>
                <configuration>
                    <activeRecipes>
                        <recipe>io.quarkus.maven.javax.managed</recipe>
                        <recipe>io.quarkus.maven.javax.versions</recipe>
                        <recipe>io.quarkus.jakarta-versions</recipe>
                    </activeRecipes>
                </configuration>
            </plugin>
```

:warning: Not all recipes are equal and please be extremely careful when adjusting existing recipes.

In particular, be extremely care with the `io.quarkus.jakarta-versions` recipe:

- It is used for several POM files
- It shouldn't add any content that is not common to all these POM files (for instance, adding some new dependencies is forbidden as there is a good chance you don't want to add them in all the POM adjusted by `io.quarkus.jakarta-versions`)
- If you need to go further than just updating versions, create a specific recipe such as `io.quarkus.jakarta-jaxrs-jaxb`

### Eclipse Transformer

The transformer phase is done at a more granular level but, typically, for now we transform the whole `extensions` subdirectory at once.
There's not much to say about this phase.
AFAICS, it just works.

## Experimenting

It is recommended to do the experiments in a separate copy of the Quarkus repository and:

- do the adjustments in a work branch of your original Quarkus repository, for instance `jakarta-work`
- commit in this branch
- then sync the branch and run the migration scripts in the separate `quarkus-jakarta` repository

Note that the `transform.sh` script will remove all `999-SNAPSHOT` Quarkus artifacts that are in your local Maven repository.

That way, you can easily reinitialize your `quarkus-jakarta` copy after having attempted a migration.

You can for instance use the following approach:

- create a `quarkus-jakarta` copy of your `quarkus` repository
- add a `jakarta` remote to this `quarkus-jakarta` copy: `git remote add jakarta ../quarkus/`
- then you can run the following command to perform a full migration: `rm -rf ~/.m2/repository/io/quarkus && git checkout -- . && git pull jakarta jakarta-work && ./jakarta/transform.sh ; paplay /usr/share/sounds/freedesktop/stereo/complete.oga` (the last part plays a sound on Linux when done, if you use macOS, remove it)
