### Command Definition

- Annotate the main class with `@Command` and implement `Runnable` or `Callable<Integer>`.
- Use `@CommandLine.Option(names = "-n")` for options.
- Use `@CommandLine.Parameters` for positional arguments.

### Quarkus Integration

- CDI injection works in command classes.
- Use `@QuarkusMain` if you need custom entry point logic.

### Subcommands

- Define subcommands with `@Command(subcommands = {SubCmd.class})`.

### Testing

- Use `@QuarkusTest` with `@QuarkusMainTest` — launch the CLI and assert exit codes.

### Common Pitfalls

- Picocli apps use `QuarkusApplication`, not the standard HTTP server.
- Do NOT add REST extensions unless you need an HTTP server alongside the CLI.
