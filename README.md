# Blade

> [!IMPORTANT]
> Blade has gone through a large internal refactor and some APIs have changed.

> [!IMPORTANT]
> Blade is now published to Maven Central. Please use the new group ID `io.github.vaperion.blade` (instead of
> `com.github.vaperion.blade`). Versioning has been reset to 1.0.0 for this
> release.

Blade is an easy-to-use command framework based on annotations.

If you make any changes or improvements to the project, please consider making a pull request to merge your changes back
into the upstream project.
If you find any issues please open an issue.

This project follows [Semantic Versioning](https://semver.org/).

## YourKit

YourKit supports open source projects with innovative and intelligent tools for monitoring and profiling Java and .NET
applications. YourKit is the creator
of [YourKit Java Profiler](https://www.yourkit.com/java/profiler/), [YourKit .NET Profiler](https://www.yourkit.com/.net/profiler/)
and [YourKit YouMonitor](https://www.yourkit.com/youmonitor/).

![YourKit](https://www.yourkit.com/images/yklogo.png)

## Supported Platforms

- Fabric: use the `fabric` artifact
- Paper 1.13+: use the `paper` artifact
- Bukkit (and Paper <1.13): use the `bukkit` artifact
- Velocity: use the `velocity` artifact

> [!WARNING]
> For Fabric, make sure to use `include(modImplementation("..."))` to add Blade as a jar-in-jar dependency. You'll have
> to include the `fabric`, `brigadier`, and `core` modules as Fabric Loom doesn't resolve transitive dependencies.

> [!TIP]
> Blade uses [Lucko's Fabric Permissions API](https://github.com/lucko/fabric-permissions-api) for permission checks on
> Fabric. Plugins such as LuckPerms provide support for this API.

## Usage

### Maven

```xml

<dependencies>
    <dependency>
        <groupId>io.github.vaperion.blade</groupId>
        <artifactId>PLATFORM</artifactId>
        <!-- Replace VERSION with your desired version -->
        <version>VERSION</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

### Gradle

```groovy
dependencies {
    // Replace VERSION with your desired version
    implementation 'io.github.vaperion.blade:PLATFORM:VERSION'
}
```

### Creating an example command

```java
public class ExampleCommand {
    @Command("example")
    @Description("The description of your command, optional.")
    @Permission("command.permission") // Optional, set to "op" to require OP
    @Hidden // Optional, hides the command from the generated help
    @Async // Optional
    @Quoted // Optional, parses quoted strings into a single argument
    public static void example(
        // Command sender, required:
        @Sender CommandSender sender,
        // Type can be: CommandSender, Player, ConsoleCommandSender
        // Or any custom type if you register a SenderProvider<T>.

        // Regular arguments:
        @Name("player") Player player,
        // You can make the argument optional (will be null if not provided):
        @Name("player") @Opt Player player,
        // or, you can make it default to the sender if not provided:
        @Name("player") @Opt(Opt.Type.SENDER) Player player,
        // Multi-word (combined) string arguments:
        @Name("message") @Greedy String message,
        // Number arguments with a range:
        @Name("amount") @Range(min = 1, max = 64) int amount,

        // You can also use custom providers for just one argument:
        @Provider(MyPlayerProvider.class) Player customPlayer,
        // And you can specify the scope (`BOTH`, `PARSER`, `SUGGESTIONS`), defaults to `BOTH`:
        @Provider(value = MyPlayerProvider.class, scope = Provider.Scope.SUGGESTIONS) Player customPlayer2,
        
        // Command flags:
        @Flag(value = 's', description = "Optional description") boolean flagSilent,
        // You can also have complex types as flags:
        @Flag('p') Player anotherPlayer
    ) {
        sender.sendMessage("(You -> " + anotherPlayer + ") $" + amount);

        if (!flagSilent) {
            player.sendMessage("(" + sender.getName() + " -> You) +$" + amount);
        } else {
            player.sendMessage("(Anonymous -> You) +$" + amount);
        }
    }
}
```

### Creating an example argument type

> [!WARNING]
> Argument provider instances must be stateless, as a single instance will be used for all commands.
> If you do have to store some state, make sure to account for that.

```java
public class Data {
    public String message;
    public boolean wasProvided;
}

public class DataArgumentProvider implements ArgumentProvider<Data> {
    @Override
    public @Nullable Data provide(@NotNull Context ctx, @NotNull InputArgument arg) {
        Data data = new Data();

        if (arg.status() == InputArgument.Status.NOT_PRESENT) {
            data.wasProvided = false;
            data.message = "Default value: " + arg.value();
        } else {
            data.wasProvided = true;
            data.message = arg.value();
        }

        // If you encounter an error while parsing, you may:
        throw new BladeUsageMessage(); // to show the usage message
        // or, to fail the command execution with a custom message:
        throw BladeParseError.fatal("Custom error message");
        // or, to allow recovery if the argument is optional:
        throw BladeParseError.recoverable("Custom error message");

        return data;
    }

    @Override
    public void suggest(@NotNull Context ctx, @NotNull InputArgument arg, @NotNull SuggestionsBuilder suggestions) {
        suggestions.suggest("example");
    }
}
```

### Registering your commands and argument types

```java
public class MyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        Blade.forPlatform(new BladeBukkitPlatform(this))
            .config(cfg -> {
                cfg.commandQualifier("myplugin"); // Optional, defaults to your plugin's name
                cfg.defaultPermissionMessage("No permission!"); // Optional
            })
            .bind(binder -> {
                binder.release(Player.class); // To remove the default provider
                binder.bind(Player.class, new MyPlayerProvider()); // To add your own
                binder.bindSender(MySender.class, new MySenderProvider()); // To add your own sender provider
            })
            .build()
            // Now, you can register all commands in a package (including sub-packages):
            .registerPackage(MyPlugin.class, "com.example.commands")
            // or, you can register them individually:
            .register(ExampleCommand.class).register(AnotherCommand.class)
        ;
    }
}
```
