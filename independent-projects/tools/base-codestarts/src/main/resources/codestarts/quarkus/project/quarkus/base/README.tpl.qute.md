# {project.artifact-id} Project

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
{buildtool.cli} {buildtool.cmd.dev}
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

## Packaging and running the application

The application can be packaged using:
```shell script
{buildtool.cli} {buildtool.cmd.package}
```
It produces the `quarkus-run.jar` file in the `{buildtool.build-dir}/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `{buildtool.build-dir}/quarkus-app/lib/` directory.

The application is now runnable using `java -jar {buildtool.build-dir}/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:
```shell script
{buildtool.cli} {buildtool.cmd.package-uber-jar}
```

The application, packaged as an _über-jar_, is now runnable using `java -jar {buildtool.build-dir}/*-runner.jar`.

## Creating a native executable

You can create a native executable using: 
```shell script
{buildtool.cli} {buildtool.cmd.package-native}
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: 
```shell script
{buildtool.cli} {buildtool.cmd.package-native-container}
```

You can then execute your native executable with: `./{buildtool.build-dir}/{project.artifact-id}-{project.version}-runner`

If you want to learn more about building native executables, please consult {buildtool.guide}.
{#if input.selected-extensions}

## Related Guides

{#for ext in input.selected-extensions}
{#if ext.guide}
- {ext.name} ([guide]({ext.guide})): {ext.description}
{/if}
{/for}
{/if}
{#if input.provided-code}

## Provided Code
{/if}
