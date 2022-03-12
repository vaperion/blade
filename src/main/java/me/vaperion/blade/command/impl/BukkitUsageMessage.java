package me.vaperion.blade.command.impl;

import me.vaperion.blade.annotation.Flag;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.command.BladeParameter.CommandParameter;
import me.vaperion.blade.command.BladeParameter.FlagParameter;
import me.vaperion.blade.command.UsageMessage;
import me.vaperion.blade.context.BladeContext;
import me.vaperion.blade.utils.MessageBuilder;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class BukkitUsageMessage implements UsageMessage {

    private final MessageBuilder messageBuilder;

    public BukkitUsageMessage(BladeCommand command) {
        this.messageBuilder = new MessageBuilder("Usage: /").color(ChatColor.RED)
              .hoverWithColor(ChatColor.GRAY, command.getDescription())
              .append(command.getUsageAlias().isEmpty() ? command.getAliases()[0] : command.getUsageAlias());

        if (!command.getCustomUsage().isEmpty()) {
            this.messageBuilder.append(" ").append(command.getCustomUsage());
            return;
        }

        // Add flag parameters
        boolean first = true;
        for (FlagParameter flagParameter : command.getFlagParameters()) {
            Flag flag = flagParameter.getFlag();

            if (first) {
                this.messageBuilder.append(" (").reset().color(ChatColor.RED).hoverWithColor(ChatColor.GRAY, command.getDescription());
                first = false;
            } else {
                this.messageBuilder.append(" | ").reset().color(ChatColor.RED).hoverWithColor(ChatColor.GRAY, command.getDescription());
            }

            this.messageBuilder
                  .append("-" + flag.value() + (flagParameter.isBooleanFlag() ? "" : " <" + flagParameter.getName() + ">"))
                  .color(ChatColor.AQUA)
                  .hoverWithColor(ChatColor.GRAY, flag.description());
        }
        if (!first) this.messageBuilder.append(")").reset().color(ChatColor.RED).hoverWithColor(ChatColor.GRAY, command.getDescription());

        // Add real parameters
        for (CommandParameter commandParameter : command.getCommandParameters()) {
            this.messageBuilder.append(" ");

            this.messageBuilder.append(commandParameter.isOptional() ? "(" : "<");
            this.messageBuilder.append(commandParameter.getName());
            if (commandParameter.isCombined()) this.messageBuilder.append("...");
            this.messageBuilder.append(commandParameter.isOptional() ? ")" : ">");
        }

        // Add extra usage
        if (!command.getExtraUsageData().isEmpty()) {
            this.messageBuilder.append(" ").append(command.getExtraUsageData().trim());
        }
    }

    @Override
    public void sendTo(@NotNull BladeContext context) {
        messageBuilder.sendTo((CommandSender) context.sender().getBackingSender());
    }

    @NotNull
    @Override
    public String toString() {
        return messageBuilder.toStringFormat();
    }
}
