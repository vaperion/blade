# Blade

:warning: Blade only supports Java 8-11 due to the use of reflection!

Blade is an easy-to-use command framework based on annotations. It currently only supports Bukkit, but it can be easily extended to more platforms.
To use Blade, you simply have to include it as a dependency and shade it into your final jar.

If you make any changes or improvements to the project, please consider making a pull request to merge your changes back into the upstream project.
This project is in its early stages, if you find any issues please open an issue.

This project follows [Semantic Versioning](https://semver.org/).

## YourKit

YourKit supports open source projects with innovative and intelligent tools for monitoring and profiling Java and .NET applications. YourKit is the creator of [YourKit Java Profiler](https://www.yourkit.com/java/profiler/), [YourKit .NET Profiler](https://www.yourkit.com/.net/profiler/) and [YourKit YouMonitor](https://www.yourkit.com/youmonitor/).

![YourKit](https://www.yourkit.com/images/yklogo.png)

## Using Blade

Maven
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.vaperion</groupId>
        <artifactId>blade</artifactId>
        <version>2.1.8</version>
        <scope>compile</scope>
    </dependency>
</dependencies>
```

Gradle
```groovy
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}

dependencies {
    implementation 'com.github.vaperion:blade:2.1.8'
}
```

### Example code

Initializing Blade:

```java
import me.vaperion.blade.Blade;
import me.vaperion.blade.bindings.impl.BukkitBindings;
import me.vaperion.blade.container.impl.BukkitCommandContainer;
import org.bukkit.plugin.java.JavaPlugin;

public class ExamplePlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        Blade.of()
                .fallbackPrefix("fallbackPrefix")
                .containerCreator(BukkitCommandContainer.CREATOR)
                .binding(new BukkitBindings())
                .build()
                .register(ExampleCommand.class);
    }
}
```

Overriding bukkit commands:
```java
Blade.of()
        ...
        .overrideCommands(true)
        ...;
```

Setting a custom tab completer:
```java
Blade.of()
        ...
        .tabCompleter(new ProtocolLibTabCompleter(this))
        ...;
```

Registering a type provider without Bindings:
```java
Blade.of()
        ...
        .bind(Example.class, new BladeProvider<Example>() {...})
        ...;
```

Example commands:

```java
import me.vaperion.blade.annotation.*;
import org.bukkit.entity.Player;

public class ExampleCommand {

    @Command(value = {"ban", "go away"}, async = true, quoted = false, description = "Ban a player")
    @Permission(value = "blade.command.ban", message = "You are not allowed to execute this command.")
    public static void ban(@Sender Player sender,
                           @Flag(value = 's', description = "Silently ban the player") boolean silent,
                           @Name("target") Player target,
                           @Name("reason") @Combined String reason) {
        sender.sendMessage("Silent: " + silent);
        sender.sendMessage("Target: " + target.getName());
        sender.sendMessage("Reason: " + reason);
    }

    @Command("test")
    public static void test(@Sender Player sender,
                            @Range(min = 18) int age,
                            @Optional("100") @Range(max = 100000) double balance) {
        sender.sendMessage("Age: " + age);
        sender.sendMessage("Balance: " + balance);
    }

    @Command({"balance", "bal"})
    public static void balance(@Sender Player sender,
                               @Optional Player target) {
        if (target == null) {
            sender.sendMessage("Your balance is: $100");
        } else {
            sender.sendMessage(target.getName() + "'s balance is: $100");
        }
    }
}
```

Example custom tab completer with Netty:

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
