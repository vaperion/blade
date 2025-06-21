# Blade

Blade is an easy-to-use command framework based on annotations. It currently supports Bukkit and Velocity.

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

## Usage

First, you need to set up the dependency. You can do this by following the instructions below depending on your build
system.
You also need to shade the library into your final jar. You can do this by choosing a shade plugin for your build system
and shading the library into your final jar.

### Maven

```xml

<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
<dependency>
    <groupId>com.github.vaperion.blade</groupId>
    <!-- Replace PLATFORM with the platform you want to use: -->
    <!-- * paper: 1.13+ Paper servers -->
    <!-- * bukkit: <1.13 Paper servers & all Bukkit servers -->
    <!-- * velocity: Velocity servers -->
    <artifactId>PLATFORM</artifactId>
    <!-- Replace VERSION with your desired version -->
    <version>VERSION</version>
    <scope>provided</scope>
</dependency>
</dependencies>
```

### Gradle

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    // Replace PLATFORM with the platform you want to use: bukkit or velocity
    // Replace VERSION with your desired version
    implementation 'com.github.vaperion.blade:PLATFORM:VERSION'
}
```

## Getting Started

### Creating an example command

```java
public class ExampleCommand {
    @Command("example")
    @Description("The description of your command, optional.")
    @Permission("command.permission") // Optional, set to "op" to require OP
    @Hidden // Optional, hides the command from the generated help
    @Async // Optional
    @ParseQuotes // Optional, parses quoted strings into a single argument
    public static void example(
          // Command sender, required:
          @Sender CommandSender sender,
          // Type can be: CommandSender, Player, ConsoleCommandSender

          // Regular arguments:
          @Name("player") Player player,
          // You can make the argument optional:
          @Name("player") @Optional Player player,
          // or, you can make it default to the sender if not provided:
          @Name("player") @Optional("self") Player player,
          // Multi-word (combined) string arguments:
          @Name("message") @Text String message,
          // Number arguments with a range:
          @Name("amount") @Range(min = 1, max = 64) int amount,

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

```java
public class Data {
    public String message;
    public boolean wasProvided;
}

public class DataArgumentProvider implements ArgumentProvider<Data> {
    @Override
    public @Nullable Data provide(@NotNull Context ctx, @NotNull Argument arg) throws BladeExitMessage {
        Data data = new Data();

        if (arg.getType() == Argument.Type.OPTIONAL) {
            data.wasProvided = false;
            data.message = "Default value: " + arg.getString();
        } else {
            data.wasProvided = true;
            data.message = arg.getString();
        }

        // If you encounter an error while parsing, you may:
        throw new BladeUsageMessage(); // to show the usage message
        // or, return a custom error message:
        throw new BladeExitMessage("Custom error message");

        // If you couldn't parse their input, you can check if the argument is optional:
        if (arg.getParameter().ignoreFailedArgumentParse()) {
            // If it is, you can return null
            return null;
        } else {
            // If it isn't, you can throw an exception to show a custom message
            throw new BladeExitMessage("Sorry, I couldn't parse your input.");
        }

        return data;
    }

    @Override
    public @NotNull List<String> suggest(@NotNull Context ctx, @NotNull Argument arg) throws BladeExitMessage {
        return Collections.singletonList("example");
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
                  cfg.setFallbackPrefix("myplugin"); // Optional, defaults to your plugin's name
                  cfg.setDefaultPermissionMessage("No permission!"); // Optional
              })
              .bind(binder -> {
                  binder.release(Player.class); // To remove the default provider
                  binder.bind(Player.class, new MyPlayerProvider()); // To add your own
              })
              .build()
              // Now, you can register all commands in a package:
              .registerPackage(MyPlugin.class, "com.example.commands")
              // or, you can register them individually:
              .register(ExampleCommand.class).register(AnotherCommand.class)
        ;
    }
}
```
