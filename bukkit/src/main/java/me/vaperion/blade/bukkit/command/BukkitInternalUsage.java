package me.vaperion.blade.bukkit.command;

import me.vaperion.blade.annotation.parameter.Flag;
import me.vaperion.blade.bukkit.util.MessageBuilder;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.command.InternalUsage;
import me.vaperion.blade.command.parameter.DefinedArgument;
import me.vaperion.blade.command.parameter.DefinedFlag;
import me.vaperion.blade.context.Context;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public final class BukkitInternalUsage implements InternalUsage<String> {

    private final BaseComponent[] components;
    private String toString;

    public BukkitInternalUsage(BladeCommand command) {
        this(command, true);
    }

    public BukkitInternalUsage(BladeCommand command, boolean addPrefix) {
        MessageBuilder messageBuilder = new MessageBuilder((addPrefix ? "Usage: " : "") + "/").color(ChatColor.RED)
            .hoverWithColor(ChatColor.GRAY, command.description())
            .append(command.mainLabel());

        if (!command.customUsage().isEmpty()) {
            messageBuilder.append(" ").append(command.customUsage());
            this.components = messageBuilder.build();
            return;
        }

        // Add flag parameters
        boolean first = true;
        for (DefinedFlag definedFlag : command.flags()) {
            Flag flag = definedFlag.flag();

            if (first) {
                messageBuilder.append(" (").reset().color(ChatColor.RED)
                    .hoverWithColor(ChatColor.GRAY, command.description());
                first = false;
            } else {
                messageBuilder.append(" | ").reset().color(ChatColor.RED)
                    .hoverWithColor(ChatColor.GRAY, command.description());
            }

            messageBuilder
                .append("-" + flag.value() + (definedFlag.isBooleanFlag() ? "" : " <" + definedFlag.name() + ">"))
                .color(ChatColor.AQUA)
                .hoverWithColor(ChatColor.GRAY, flag.description());
        }

        if (!first) {
            messageBuilder.append(")").reset().color(ChatColor.RED)
                .hoverWithColor(ChatColor.GRAY, command.description());
        }

        // Add real parameters
        for (DefinedArgument arg : command.arguments()) {
            messageBuilder.append(" ");

            messageBuilder.append(arg.isOptional() ? "(" : "<");
            messageBuilder.append(arg.name());
            if (arg.isGreedy()) messageBuilder.append("...");
            messageBuilder.append(arg.isOptional() ? ")" : ">");
        }

        // Add extra usage
        if (!command.extraUsageData().isEmpty()) {
            messageBuilder.append(" ").append(command.extraUsageData().trim());
        }

        this.components = messageBuilder.build();
    }

    @Override
    public @NotNull String message() {
        return MessageBuilder.toStringFormat(this.components);
    }

    @Override
    public void sendTo(@NotNull Context context) {
        MessageBuilder.send((CommandSender) context.sender().rawSender(), components);
    }
}
