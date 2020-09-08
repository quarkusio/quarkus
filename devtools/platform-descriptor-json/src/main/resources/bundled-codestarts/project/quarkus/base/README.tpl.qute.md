# {project.artifact-id} project

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
{buildtool.cmd.dev}
```

## Packaging and running the application

The application can be packaged using:
```shell script
{buildtool.cmd.package}
```
It produces the `{project.artifact-id}-{project.version}-runner.jar` file in the `/{buildtool.build-dir}` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `{buildtool.build-dir}/lib` directory.
If you want to build an _über-jar_, just add the `--uber-jar` option to the command line:
```shell script
{buildtool.cmd.package-uberjar}
```

The application is now runnable using `java -jar {buildtool.build-dir}/{project.artifact-id}-{project.version}-runner.jar`.

## Creating a native executable

You can create a native executable using: 
```shell script
{buildtool.cmd.package-native}
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: 
```shell script
{buildtool.cmd.package-native-container}
```

You can then execute your native executable with: `./{buildtool.build-dir}/{project.artifact-id}-{project.version}-runner`

If you want to learn more about building native executables, please consult {buildtool.guide}.

