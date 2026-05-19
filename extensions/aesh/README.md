# Quarkus Aesh Extension

This extension provides integration between Quarkus and [Aesh](https://github.com/aeshell/aesh), enabling you to build powerful interactive command-line applications with features like tab completion, command history, and sub-command navigation.

## Installation

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-aesh</artifactId>
</dependency>
```

## Quick Start: Interactive CLI

Create commands with `@CommandDefinition` and `@CliCommand`:

```java
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import io.quarkus.aesh.runtime.annotations.CliCommand;

@CommandDefinition(name = "greet", description = "Greet someone")
@CliCommand
public class GreetCommand implements Command<CommandInvocation> {

    @Option(shortName = 'n', name = "name", defaultValue = "World")
    private String name;

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        invocation.println("Hello, " + name + "!");
        return CommandResult.SUCCESS;
    }
}

@CommandDefinition(name = "calc", description = "Calculate sum")
@CliCommand
public class CalcCommand implements Command<CommandInvocation> {

    @Option(shortName = 'a', defaultValue = "0")
    private int a;

    @Option(shortName = 'b', defaultValue = "0")
    private int b;

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        invocation.println("Result: " + (a + b));
        return CommandResult.SUCCESS;
    }
}
```

Run your application and you get an interactive shell:

```
$ java -jar target/quarkus-app/quarkus-run.jar
[quarkus]$ greet --name John
Hello, John!
[quarkus]$ calc -a 5 -b 3
Result: 8
[quarkus]$ exit
```

## Sub-Command Mode

Sub-command mode lets users enter an interactive context for group commands. This is ideal for CLI tools with deep command hierarchies.

### Defining Hierarchical Commands

```java
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.CommandDefinition;
import org.aesh.command.option.Option;
import org.aesh.command.option.Argument;
import org.aesh.command.option.ParentCommand;

@GroupCommandDefinition(name = "module", description = "Module management",
        groupCommands = { AddModuleCommand.class, ListModulesCommand.class, TagCommand.class })
@CliCommand
public class ModuleCommand implements Command<CommandInvocation> {

    @Argument(description = "Module name")
    private String moduleName;

    @Option(name = "verbose", shortName = 'v', hasValue = false)
    private boolean verbose;

    public String getModuleName() { return moduleName; }
    public boolean isVerbose() { return verbose; }

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        // Displayed when entering sub-command mode without a subcommand
        invocation.println("Module: " + (moduleName != null ? moduleName : "(none)"));
        return CommandResult.SUCCESS;
    }
}

@CommandDefinition(name = "add", description = "Add a new module")
public class AddModuleCommand implements Command<CommandInvocation> {

    @ParentCommand
    private ModuleCommand parent;  // Access parent's options

    @Option(name = "name", required = true)
    private String name;

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        if (parent.isVerbose()) {
            invocation.println("[VERBOSE] Creating module...");
        }
        invocation.println("Added module: " + name);
        return CommandResult.SUCCESS;
    }
}

@CommandDefinition(name = "list", description = "List modules")
public class ListModulesCommand implements Command<CommandInvocation> {
    @Override
    public CommandResult execute(CommandInvocation invocation) {
        invocation.println("Modules: core, web, data");
        return CommandResult.SUCCESS;
    }
}
```

### Using Sub-Command Mode

```bash
[quarkus]$ module myapp --verbose    # Enter sub-command mode with context
Entering module mode:
  moduleName: myapp
  verbose: true
Type 'exit' to return.

module[myapp]> add --name core       # Subcommands have access to parent context
[VERBOSE] Creating module...
Added module: core

module[myapp]> list
Modules: core, web, data

module[myapp]> exit                  # Return to main prompt
[quarkus]$
```

### Direct Execution (No Interactive Mode)

When you specify a subcommand, it executes directly without entering interactive mode:

```bash
[quarkus]$ module myapp add --name core
Added module: core
[quarkus]$                           # Immediately back to main prompt
```

### Sub-Command Mode Configuration

```properties
# Enable/disable sub-command mode (default: true)
quarkus.aesh.sub-command-mode.enabled=true

# Exit commands (default: "exit" and "..")
quarkus.aesh.sub-command-mode.exit-command=exit
quarkus.aesh.sub-command-mode.alternative-exit-command=..

# Prompt configuration
quarkus.aesh.sub-command-mode.context-separator=:
quarkus.aesh.sub-command-mode.show-argument-in-prompt=true
quarkus.aesh.sub-command-mode.show-context-on-entry=true
```

## CDI Integration

Commands are automatically CDI beans and can inject services:

```java
@CommandDefinition(name = "status", description = "Show status")
@CliCommand
public class StatusCommand implements Command<CommandInvocation> {

    @Inject
    DatabaseService database;

    @Inject
    CacheService cache;

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        invocation.println("Database: " + database.getStatus());
        invocation.println("Cache: " + cache.getStatus());
        return CommandResult.SUCCESS;
    }
}
```

## Configuration

### Console Settings

```properties
# Prompt (default: "[quarkus]$ ")
quarkus.aesh.prompt=[myapp]$

# Built-in exit command (default: true)
quarkus.aesh.add-exit-command=true

# Command aliasing (default: false)
quarkus.aesh.enable-alias=true

# Man page support (default: false)
quarkus.aesh.enable-man=true

# Command history
quarkus.aesh.persist-history=true
quarkus.aesh.history-file=/path/to/.myapp_history
quarkus.aesh.history-size=1000
```

### Custom Settings

For advanced configuration, implement `CliSettings`:

```java
import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.aesh.runtime.CliSettings;
import org.aesh.command.settings.SettingsBuilder;

@ApplicationScoped
public class MyCliSettings implements CliSettings {

    @Override
    public void customize(SettingsBuilder<?, ?, ?, ?, ?, ?> builder) {
        builder.enableOperatorParser(true)
               .enableSearchInPaging(true)
               .setPagingPrompt("--More--");
    }
}
```

## Remote Terminal Access

The extension supports remote terminal access via WebSocket and SSH, allowing users to interact with a running console-mode application from a browser or SSH client.

### WebSocket Terminal

Add the dependency:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-aesh-websocket</artifactId>
</dependency>
```

Access the terminal at `http://localhost:8080/aesh/index.html`. Disable with:

```properties
quarkus.aesh.websocket.enabled=false
```

### SSH Terminal

Add the dependency:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-aesh-ssh</artifactId>
</dependency>
```

Connect with `ssh -p 2222 localhost`. Configure with:

```properties
quarkus.aesh.ssh.port=2222
quarkus.aesh.ssh.host=localhost
quarkus.aesh.ssh.password=mysecret
quarkus.aesh.ssh.enabled=true
```

> **Note:** These features are intended for development. Secure them appropriately in production.

## Aesh 3.0 Features

### @ParentCommand

Subcommands can access their parent command's options:

```java
@CommandDefinition(name = "clone", description = "Clone repository")
public class CloneCommand implements Command<CommandInvocation> {

    @ParentCommand
    private GitCommand parent;

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        if (parent.isVerbose()) {
            invocation.println("Verbose mode enabled");
        }
        // ...
    }
}
```

### Inherited Options

Options on parent commands are automatically available to subcommands.

### Negatable Booleans

Boolean options can be negated with `--no-` prefix:

```java
@Option(name = "color", defaultValue = "true")
private boolean color;
// Disable with: --no-color
```

## Single Command Mode

For simple CLI tools that execute one command and exit (like picocli), use `@TopCommand`:

```java
@CommandDefinition(name = "greet", description = "Greet someone")
@TopCommand
public class GreetCommand implements Command<CommandInvocation> {

    @Option(shortName = 'n', name = "name", defaultValue = "World")
    private String name;

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        invocation.println("Hello, " + name + "!");
        return CommandResult.SUCCESS;
    }
}
```

Run: `java -jar app.jar --name John`

### With Subcommands (git-style)

```java
@GroupCommandDefinition(name = "myapp", description = "My application",
        groupCommands = { GreetCommand.class, CalcCommand.class })
@TopCommand
public class MyAppCommand implements Command<CommandInvocation> {
    @Override
    public CommandResult execute(CommandInvocation invocation) {
        invocation.println("Usage: myapp <command>");
        return CommandResult.SUCCESS;
    }
}
```

Run: `java -jar app.jar greet --name John`

## Execution Mode

The extension auto-detects the appropriate mode:

| Annotations | Mode |
|------------|------|
| `@CliCommand` on multiple commands | Console (interactive shell) |
| `@TopCommand` or `@GroupCommandDefinition` | Runtime (execute and exit) |
| Single `@CommandDefinition` | Runtime |
| Multiple `@CommandDefinition` without grouping | Console |

Force a specific mode:

```properties
quarkus.aesh.mode=console  # or 'runtime' or 'auto'
```

