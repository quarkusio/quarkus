# Devcontainers support for Quarkus

## Motivation

Sometimes, you don´t have the right dev environment to explore and contribute in the `Quarkus`project. With this idea in Mind, `Devcontainers` was added to use the capabilities from Cloud Environments like `Github Codespaces`.

**Note:** Not use in local DevContainer with Quarkus because it is massive and you are adding another abstraction layer.

## Getting Started

If you visit the project in Github: https://github.com/quarkusio/quarkus in the main view, exist a Green button named `Code` if you click in the dropdown, you will see the option `Codespaces` so, if you click in the button `Open in Codespaces`, you will open a new Tab in your web browser to load Quarkus in Codespaces.

## Benefits

Quarkus project has plenty extensions, if you are interesting to review any extension, you could use DevContainers to review in an easy & quick way.

**Examples:**

```bash
#High performance Data Structures
./mvnw clean verify -pl extensions/agroal -am

#Dependency Injection Engine in Quarkus
./mvnw clean verify  -pl extensions/arc -am

#Devservices
./mvnw clean verify  -pl extensions/devservices -am

#Panache
./mvnw clean verify  -pl extensions/panache -am
```

Enjoy!

## References

- https://containers.dev/
- https://github.com/features/codespaces
- https://github.com/codespaces