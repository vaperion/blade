# Blade

Blade is an easy-to-use command framework based on annotations. It currently supports Bukkit and Velocity.
To use Blade, you simply have to include it as a dependency and shade it into your final jar.

If you make any changes or improvements to the project, please consider making a pull request to merge your changes back into the upstream project.
If you find any issues please open an issue.

This project follows [Semantic Versioning](https://semver.org/).

## YourKit

YourKit supports open source projects with innovative and intelligent tools for monitoring and profiling Java and .NET applications. YourKit is the creator of [YourKit Java Profiler](https://www.yourkit.com/java/profiler/), [YourKit .NET Profiler](https://www.yourkit.com/.net/profiler/) and [YourKit YouMonitor](https://www.yourkit.com/youmonitor/).

![YourKit](https://www.yourkit.com/images/yklogo.png)

## Using Blade

Include the jitpack repository using the package manager of your choice:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```
```groovy
repositories {
    maven { url 'https://jitpack.io' }
}
```

And finally, include the dependency:

[![Release](https://jitpack.io/v/vaperion/blade.svg)](https://jitpack.io/#vaperion/blade)

```xml
<dependencies>
    <dependency>
        <groupId>com.github.vaperion</groupId>
        <artifactId>blade</artifactId>
        <version>VERSION</version>
        <scope>compile</scope>
    </dependency>
</dependencies>
```
```groovy
dependencies {
    implementation 'com.github.vaperion:blade:VERSION'
}
```

### Creating your first bukkit command

```java
package you.developer.exampleplugin;

import me.vaperion.blade.Blade;
import me.vaperion.blade.annotation.*;
import me.vaperion.blade.argument.BladeProvider;
import me.vaperion.blade.bindings.impl.BukkitBindings;
import me.vaperion.blade.container.impl.BukkitCommandContainer;
import org.bukkit.plugin.java.JavaPlugin;

public class ExamplePlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        Blade.of() // Create a new Blade builder
            .bukkitPlugin(this) // Pass your plugin instance to Blade (optional)
            .fallbackPrefix("example") // Set the fallback prefix - shows up as `/example:ban`
            .containerCreator(BukkitCommandContainer.CREATOR) // Set the container creator
            .overrideCommands(true) // Should blade override already registered commands? (optional, defaults to false)
            .defaultPermissionMessage("No permission.") // Set the default permission message (optional)
            .binding(new BukkitBindings()) // Add the default bindings for Bukkit (Player, OfflinePlayer, etc.) (optional, you may call this multiple times)
            .bind(Example.class, new BladeProvider<Example>() {...}) // Bind a provider for the Example type (optional, you may call this multiple times)
            .executionTimeWarningThreshold(50) // Set the maximum time (in seconds) commands on the main thread can run for (optional, defaults to 5)
            .tabCompleter(new ProtocolLibTabCompleter() [or] new TabCompleter() {...}) // Set a custom tab completer (optional, defaults to DefaultTabCompleter)
            .helpGenerator(new NoOpHelpGenerator() [or] new HelpGenerator() {...}) // Set a custom help generator (optional, defaults to BukkitHelpGenerator)
            .build() // Finish the builder
            .register(ExampleCommand.class) // Register all static commands in the provided class
            .register(new ExampleCommand()) // Register all the non-static commands in the provided object
            .registerPackage(ExamplePlugin.class, "you.developer.exampleplugin.commands") // Register all commands in the provided package
        ;
    }
    
    @Command(
        value = ["test", "testing"], // Command aliases
        async = true, // Should the command be called asynchronously (optional, defaults to false)
        quoted = true, // Should quotes in the arguments (' and ") be parsed? (optional, defaults to false)
        hidden = true, // Should blade hide this command from the help message and tab completion? (optional, defaults to false)
        description = "This is an example command.", // Command description that shows up when hovering over the usage and in the help message (optional)
        usage = "", // Custom usage message that you want to show instead of the default generated one (optional)
        usageAlias = "test", // The command alias that should be used in the usage message (optional, defaults to the first alias in `value`)
        extraUsageData = "" // Custom data that shows up after the usage message (optional)
    )
    @Permission(
        value = "example.command.test", // The permission
        message = "" // The no permission message (optional, defaults to the one set in the Blade instance)
    )
    public static void testCommand(@Sender CommandSender sender,
                                   @Flag(value = 'h', description = "Should we say hi?") boolean sayHi,
                                   @Name("message") @Combined String message) {
        sender.sendMessage("Your message is: " + message);
        if (sayHi) sender.sendMessage("Hi!");
    }
}
```

A minimal example:
```java
package you.developer.exampleplugin;

import me.vaperion.blade.Blade;
import me.vaperion.blade.annotation.Command;
import me.vaperion.blade.argument.BladeProvider;
import me.vaperion.blade.bindings.impl.BukkitBindings;
import me.vaperion.blade.container.impl.BukkitCommandContainer;
import org.bukkit.plugin.java.JavaPlugin;

public class ExamplePlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        Blade.of()
            .bukkitPlugin(this)
            .fallbackPrefix("example")
            .containerCreator(BukkitCommandContainer.CREATOR)
            .binding(new BukkitBindings())
            .build()
            .register(ExamplePlugin.class)
        ;
    }
    
    @Command("test")
    public static void testCommand(@Sender CommandSender sender) {
        sender.sendMessage("Hello, World!");
    }
}
```

### Creating your first velocity command

```java
package you.developer.exampleplugin;

import com.google.inject.Inject;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import me.vaperion.blade.Blade;
import me.vaperion.blade.annotation.Command;
import me.vaperion.blade.argument.BladeProvider;
import me.vaperion.blade.bindings.impl.VelocityBindings;
import me.vaperion.blade.container.impl.VelocityCommandContainer;
import net.kyori.adventure.text.Component;

@Plugin(id = "exampleplugin", name = "Example Plugin", version = "0.1.0-SNAPSHOT")
public class ExamplePlugin {

    private final ProxyServer server;

    @Inject
    public ExamplePlugin(ProxyServer server) {
        this.server = server;
        this.logger = logger;

        Blade.of()
            .velocityServer(server)
            .fallbackPrefix("example")
            .containerCreator(VelocityCommandContainer.CREATOR)
            .binding(new VelocityBindings())
            .build()
            .register(ExamplePlugin.class)
        ;
    }
    
    @Command("test")
    public static void testCommand(@Sender CommandSource source) {
        source.sendMessage(Component.text("Hello, World!"));
    }
}
```

### Netty tab completer for v1_7_R4 (1.7.10)

```java
import me.vaperion.blade.service.BladeCommandService;
import me.vaperion.blade.tabcompleter.TabCompleter;
import net.minecraft.server.v1_7_R4.PacketPlayInTabComplete;
import net.minecraft.server.v1_7_R4.PacketPlayOutTabComplete;
import net.minecraft.util.io.netty.channel.ChannelDuplexHandler;
import net.minecraft.util.io.netty.channel.ChannelHandlerContext;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class CustomTabCompleter implements TabCompleter, Listener {

    private BladeCommandService commandService;

    public CustomTabCompleter(JavaPlugin plugin) {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void init(@NotNull BladeCommandService bladeCommandService) {
        this.commandService = bladeCommandService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ((CraftPlayer) player).getHandle().playerConnection.networkManager.m.pipeline()
                .addBefore("packet_handler", "blade_completer", new ChannelDuplexHandler() {
                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                        if (msg instanceof PacketPlayInTabComplete) {
                            String commandLine = ((PacketPlayInTabComplete) msg).c();
                            if (commandLine.startsWith("/")) {
                                commandLine = commandLine.substring(1);

                                List<String> suggestions = commandService.getCommandCompleter().suggest(commandLine, () -> new BukkitSender(player), (cmd) -> hasPermission(player, cmd));
                                if (suggestions != null) {
                                    ctx.writeAndFlush(new PacketPlayOutTabComplete(suggestions.toArray(new String[0])));
                                    return;
                                }
                            }
                        }

                        super.channelRead(ctx, msg);
                    }
                });
    }
}
```
