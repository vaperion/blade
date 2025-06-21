package me.vaperion.blade.bukkit.util;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings({ "unused", "deprecation" })
public class MessageBuilder {

    @NotNull
    public static String toStringFormat(@NotNull BaseComponent[] components) {
        StringBuilder strBuilder = new StringBuilder();

        for (BaseComponent component : components) {
            strBuilder.append(component.toLegacyText());
        }

        return strBuilder.toString();
    }

    public static void send(@NotNull CommandSender sender, @NotNull BaseComponent[] components) {
        if (sender instanceof Player) {
            ((Player) sender).spigot().sendMessage(components);
            return;
        }

        sender.sendMessage(toStringFormat(components));
    }

    private final ComponentBuilder builder;

    public MessageBuilder(@NotNull String mainText) {
        builder = new ComponentBuilder(mainText);
    }

    @Contract("_ -> this")
    @NotNull
    public MessageBuilder color(@NotNull ChatColor color) {
        builder.color(color);
        return this;
    }

    @Contract("_ -> this")
    @NotNull
    public MessageBuilder append(@NotNull String text) {
        builder.append(text);
        return this;
    }

    @Contract("_ -> this")
    @NotNull
    public MessageBuilder hover(@NotNull String... lines) {
        return hover(Arrays.asList(lines));
    }

    @Contract("_, _ -> this")
    @NotNull
    public MessageBuilder hoverWithColor(@NotNull ChatColor color, @NotNull String... lines) {
        return hoverWithColor(color, Arrays.asList(lines));
    }

    @Contract("_ -> this")
    @NotNull
    public MessageBuilder hover(@NotNull List<String> lines) {
        if (lines.isEmpty() || lines.stream().allMatch(String::isEmpty)) return this;
        builder.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(combineMultiLine(lines)).create()));
        return this;
    }

    @Contract("_, _ -> this")
    @NotNull
    public MessageBuilder hoverWithColor(@NotNull ChatColor color, @NotNull List<String> lines) {
        if (lines.isEmpty() || lines.stream().allMatch(String::isEmpty)) return this;
        builder.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(
            lines.stream().map(s -> color + s)
                .collect(Collectors.joining("\n"))).create()));
        return this;
    }

    @Contract("_, _ -> this")
    @NotNull
    public MessageBuilder click(@NotNull String command, boolean instant) {
        builder.event(new ClickEvent(instant
            ? ClickEvent.Action.RUN_COMMAND : ClickEvent.Action.SUGGEST_COMMAND, command));
        return this;
    }

    @Contract(" -> this")
    @NotNull
    public MessageBuilder reset() {
        builder.reset();
        return this;
    }

    @NotNull
    public BaseComponent[] build() {
        return builder.create();
    }

    @NotNull
    public String toStringFormat() {
        return toStringFormat(build());
    }

    public void sendTo(@NotNull CommandSender sender) {
        send(sender, build());
    }

    @NotNull
    private static String combineMultiLine(@NotNull String[] lines) {
        return String.join("\n", lines);
    }

    @NotNull
    private static String combineMultiLine(@NotNull List<String> lines) {
        return String.join("\n", lines);
    }
}