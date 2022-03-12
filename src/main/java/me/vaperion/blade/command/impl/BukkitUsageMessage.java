package me.vaperion.blade.command.impl;

import me.vaperion.blade.annotation.Flag;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.command.BladeParameter.CommandParameter;
import me.vaperion.blade.command.BladeParameter.FlagParameter;
import me.vaperion.blade.command.UsageMessage;
import me.vaperion.blade.context.BladeContext;
import me.vaperion.blade.utils.MessageBuilder;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class BukkitUsageMessage implements UsageMessage {

    private final BaseComponent[] components;

    public BukkitUsageMessage(BladeCommand command) {
        MessageBuilder messageBuilder = new MessageBuilder("Usage: /").color(ChatColor.RED)
              .hoverWithColor(ChatColor.GRAY, command.getDescription())
              .append(command.getUsageAlias().isEmpty() ? command.getAliases()[0] : command.getUsageAlias());

        if (!command.getCustomUsage().isEmpty()) {
            messageBuilder.append(" ").append(command.getCustomUsage());
            this.components = messageBuilder.build();
            return;
        }

        // Add flag parameters
        boolean first = true;
        for (FlagParameter flagParameter : command.getFlagParameters()) {
            Flag flag = flagParameter.getFlag();

            if (first) {
                messageBuilder.append(" (").reset().color(ChatColor.RED).hoverWithColor(ChatColor.GRAY, command.getDescription());
                first = false;
            } else {
                messageBuilder.append(" | ").reset().color(ChatColor.RED).hoverWithColor(ChatColor.GRAY, command.getDescription());
            }

            messageBuilder
                  .append("-" + flag.value() + (flagParameter.isBooleanFlag() ? "" : " <" + flagParameter.getName() + ">"))
                  .color(ChatColor.AQUA)
                  .hoverWithColor(ChatColor.GRAY, flag.description());
        }
        if (!first) messageBuilder.append(")").reset().color(ChatColor.RED).hoverWithColor(ChatColor.GRAY, command.getDescription());

        // Add real parameters
        for (CommandParameter commandParameter : command.getCommandParameters()) {
            messageBuilder.append(" ");

            messageBuilder.append(commandParameter.isOptional() ? "(" : "<");
            messageBuilder.append(commandParameter.getName());
            if (commandParameter.isCombined()) messageBuilder.append("...");
            messageBuilder.append(commandParameter.isOptional() ? ")" : ">");
        }

        // Add extra usage
        if (!command.getExtraUsageData().isEmpty()) {
            messageBuilder.append(" ").append(command.getExtraUsageData().trim());
        }

        this.components = messageBuilder.build();
    }

    @Override
    public void sendTo(@NotNull BladeContext context) {
        MessageBuilder.send((CommandSender) context.sender().getBackingSender(), components);
    }

    @NotNull
    @Override
    public String toString() {
        return MessageBuilder.toStringFormat(components);
    }
}
