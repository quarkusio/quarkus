# Jakarta migration

## Status

You can follow the status of the Jakarta migration in [the dedicated project tab](https://github.com/orgs/quarkusio/projects/13/views/20).

The build status is published in an [issue with a full report](https://github.com/quarkusio/quarkus/issues/25363).
Just scroll to the bottom for the latest.

## Testing

To test the Jakarta work, there are two approaches:

- Use the snapshots we publish nightly
- Build the `jakarta-rewrite` branch yourself

### Using snapshots

Snapshots of the `jakarta-rewrite` branch containing the transformed artifacts are published nightly on https://s01.oss.sonatype.org/content/repositories/snapshots/ .

Add the following snippet to your `pom.xml` to enable the s01 OSSRH snapshots repository:

```xml
<repositories>
    <repository>
        <name>s01 OSSRH Snapshots</name>
        <id>s01-ossrh-snapshots-repo</id>
        <url>https://s01.oss.sonatype.org/content/repositories/snapshots/</url>
        <layout>default</layout>
        <releases>
            <enabled>false</enabled>
            <updatePolicy>always</updatePolicy>
            <checksumPolicy>warn</checksumPolicy>
        </releases>
        <snapshots>
            <enabled>true</enabled>
            <updatePolicy>daily</updatePolicy>
            <checksumPolicy>fail</checksumPolicy>
        </snapshots>
    </repository>
</repositories>
```

The published artifacts have the `999-jakarta-SNAPSHOT` version so make sure to use this version in your projects.

### Build locally

Then you can build the `jakarta-rewrite` branch locally:

```
git checkout jakarta-rewrite
./mvnw -Dquickly
```

The installed artifacts have the `999-jakarta-SNAPSHOT` version so make sure to use this version in your projects.

## Transforming

This directory contains scripts and configuration files to automate the migration to Jakarta EE 10 soon.

This is a work in progress and some pieces are still missing (JPA 3.1 and Hibernate ORM 6 for instance).

### jakarta/transform.sh

`jakarta/transform.sh` is the main script to run.
It has to be run from the root of the Quarkus repository.

It is highly recommended to run the script in a specific tree (or at least a specific branch) as it will commit the changes to the current branch.

If you don't want to run the extension tests and just want to check that the build is working, you can run:

```
REWRITE_NO_TESTS=true jakarta/transform.sh
```

If you are offline and can't fetch the external projects, do:
```
REWRITE_OFFLINE=true jakarta/transform.sh
```
Obviously, you need to have run a full build before.
Also keep in mind you should run a full build from time to time to get the potential updates to OpenRewrite.

You can combine both `REWRITE_NO_TESTS` and `REWRITE_OFFLINE` in a single run if that fits you.

It consists of several steps that leverage various tools:

- [OpenRewrite](https://github.com/openrewrite/rewrite) - rewrites the POM files
- [Eclipse Transformer](https://projects.eclipse.org/projects/technology.transformer) - rewrites the classes

Note that this script also builds:

- A patched version of OpenRewrite (while waiting for integration of our patches)
- A patched version of the Rewrite Maven Plugin (to point to the patched OpenRewrite version)
- A patched version of the Kotlin Maven Plugin (to allow skipping the `main` compilation, it's required for the OpenRewrite run)

### Approach

#### OpenRewrite

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

#### Eclipse Transformer

The transformer phase is done at a more granular level but, typically, for now we transform the whole `extensions` subdirectory at once.
There's not much to say about this phase.
AFAICS, it just works.

### Experimenting

It is recommended to do the experiments in a separate copy of the Quarkus repository and:

- do the adjustments in a work branch of your original Quarkus repository, for instance `jakarta-work`
- commit in this branch
- then sync the branch and run the migration scripts in the separate `quarkus-jakarta` repository

That way, you can easily reinitialize your `quarkus-jakarta` copy after having attempted a migration.

Note that the `transform.sh` script replaces the `999-SNAPSHOT` version with `999-jakarta-SNAPSHOT` and will remove all `999-jakarta-SNAPSHOT` Quarkus artifacts that are in your local Maven repository.

You can for instance use the following approach:

- create a `quarkus-jakarta` copy of your `quarkus` repository
- add a `jakarta` remote to this `quarkus-jakarta` copy: `git remote add jakarta ../quarkus/`
- then you can run the following command to perform a full migration: `rm -rf ~/.m2/repository/io/quarkus && git checkout -- . && git pull jakarta jakarta-work && ./jakarta/transform.sh ; paplay /usr/share/sounds/freedesktop/stereo/complete.oga` (the last part plays a sound on Linux when done, if you use macOS, remove it)
