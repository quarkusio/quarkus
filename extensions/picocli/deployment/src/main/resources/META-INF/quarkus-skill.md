
### Basic Command

```java
@TopCommand
@Command(name = "myapp", mixinStandardHelpOptions = true, version = "1.0")
public class MyApp implements Runnable {
    @Option(names = {"-v", "--verbose"}, description = "Verbose output")
    boolean verbose;

    @Parameters(index = "0", description = "Input file")
    String file;

    @Override
    public void run() {
        // command logic
    }
}
```

- The `@TopCommand` class **must** implement `Runnable` or `Callable<Integer>`.
- Use `Callable<Integer>` to return custom exit codes.
- `mixinStandardHelpOptions = true` adds `--help` and `--version` automatically.

### Subcommands

```java
@TopCommand
@Command(name = "myapp", subcommands = {AddCommand.class, ListCommand.class})
public class MyApp implements Runnable {
    @Override public void run() { /* show help if no subcommand */ }
}

@Command(name = "add", description = "Add an item")
public class AddCommand implements Runnable {
    @Parameters(description = "Item name") String name;
    @Option(names = {"-p", "--priority"}) Priority priority = Priority.MEDIUM;
    @Override public void run() { /* ... */ }
}
```

### CDI Integration

- All `@Command` classes are automatically registered as `@Dependent` CDI beans.
- Constructor injection works directly — no special setup needed:
  ```java
  @Command(name = "process")
  public class ProcessCommand implements Runnable {
      private final MyService service;
      public ProcessCommand(MyService service) { this.service = service; }
      @Override public void run() { service.process(); }
  }
  ```
- Do NOT use `@ApplicationScoped` or `@Singleton` on command classes — they must be `@Dependent` (the default).
- Injected services CAN be `@ApplicationScoped`.

### Options and Parameters

```java
@Option(names = {"-o", "--output"}, description = "Output file", required = true)
String output;

@Option(names = {"-n", "--count"}, description = "Number of items", defaultValue = "10")
int count;

@Option(names = {"-i", "--ignore-case"}, description = "Case insensitive")
boolean ignoreCase;  // defaults to false

@Parameters(index = "0", description = "First positional arg")
String firstArg;

@Parameters(index = "1..*", description = "Remaining args")
List<String> remaining;
```

### Exit Codes

Use `Callable<Integer>` instead of `Runnable` to control exit codes:

```java
@Command(name = "validate")
public class ValidateCommand implements Callable<Integer> {
    @Override
    public Integer call() {
        if (valid) return 0;  // success
        return 1;             // failure
    }
}
```

### Dev Mode

CLI apps exit immediately after running. To pass arguments in dev mode:
```
mvn quarkus:dev -Dquarkus.args='subcommand --flag value'
```
Press `Space` to re-run with the same arguments.

### Testing

Inject `CommandLine.IFactory` and capture output:

```java
@QuarkusTest
class MyCommandTest {
    @Inject CommandLine.IFactory factory;

    @Test
    void testCommand() {
        var out = new StringWriter();
        var cmd = new CommandLine(MyApp.class, factory);
        cmd.setOut(new PrintWriter(out));
        int exitCode = cmd.execute("add", "--priority", "HIGH", "My Task");
        assertEquals(0, exitCode);
        assertTrue(out.toString().contains("Added"));
    }
}
```

- Use `factory` to create `CommandLine` — this enables CDI injection in commands.
- Use `cmd.setOut()` / `cmd.setErr()` to capture output for assertions.
- In commands, print via `spec.commandLine().getOut()` (not `System.out`) for testable output:
  ```java
  @Spec CommandSpec spec;
  public void run() { spec.commandLine().getOut().println("output"); }
  ```

### Common Pitfalls

- The `@TopCommand` class must implement `Runnable` or `Callable` — an empty class causes `UnsatisfiedResolutionException`.
- Command classes must be `@Dependent` (the default) — do not add `@ApplicationScoped`.
- In tests, always use the injected `IFactory` when creating `CommandLine` — otherwise CDI injection won't work in commands.
- For long-running CLI apps that also serve HTTP, call `Quarkus.waitForExit()` in the `run()` method.
