package me.vaperion.blade.command.impl;

import com.velocitypowered.api.command.CommandSource;
import me.vaperion.blade.annotation.Flag;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.command.BladeParameter;
import me.vaperion.blade.command.BladeParameter.CommandParameter;
import me.vaperion.blade.command.UsageMessage;
import me.vaperion.blade.context.BladeContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

public class VelocityUsageMessage implements UsageMessage {

    private final Component component;

    public VelocityUsageMessage(BladeCommand command) {
        Component component = Component.text("Usage: /").color(NamedTextColor.RED)
              .hoverEvent(HoverEvent.showText(
                    Component.text(command.getDescription()).color(NamedTextColor.GRAY))
              ).append(
                    Component.text(command.getUsageAlias().isEmpty() ? command.getAliases()[0] : command.getUsageAlias())
              );

        if (!command.getCustomUsage().isEmpty()) {
            this.component = component.append(
                  Component.text(command.getCustomUsage())
            );
            return;
        }

        // Add flag parameters
        boolean first = true;
        for (BladeParameter.FlagParameter flagParameter : command.getFlagParameters()) {
            Flag flag = flagParameter.getFlag();

            if (first) {
                component = component.append(
                      Component.text(" (").color(NamedTextColor.RED)
                );
                first = false;
            } else {
                component = component.append(
                      Component.text(" | ").color(NamedTextColor.RED)
                ).hoverEvent(HoverEvent.showText(
                      Component.text(command.getDescription()).color(NamedTextColor.GRAY))
                );
            }

            component = component.append(
                        Component.text("-" + flag.value() + (flagParameter.isBooleanFlag() ? "" : " <" + flagParameter.getName() + ">"))
                  ).color(NamedTextColor.AQUA)
                  .hoverEvent(HoverEvent.showText(
                        Component.text(flag.description()).color(NamedTextColor.GRAY))
                  );
        }
        if (!first) component = component.append(
              Component.text(")").color(NamedTextColor.RED)
        ).hoverEvent(HoverEvent.showText(
              Component.text(command.getDescription()).color(NamedTextColor.GRAY))
        );

        // Add real parameters
        for (CommandParameter commandParameter : command.getCommandParameters()) {
            component = component.append(
                  Component.text(" ")
            );

            component = component.append(
                  Component.text(commandParameter.isOptional() ? "(" : "<")
            );
            component = component.append(
                  Component.text(commandParameter.getName())
            );
            if (commandParameter.isCombined()) component = component.append(
                  Component.text("...")
            );
            component = component.append(
                  Component.text(commandParameter.isOptional() ? ")" : ">")
            );
        }

        // Add extra usage
        if (!command.getExtraUsageData().isEmpty()) {
            component = component.append(
                  Component.text(" " + command.getExtraUsageData().trim())
            );
        }

        this.component = component;
    }

    @Override
    public void sendTo(@NotNull BladeContext context) {
        ((CommandSource) context.sender().getBackingSender()).sendMessage(this.component);
    }

    @NotNull
    @Override
    public String toString() {
        return LegacyComponentSerializer.legacyAmpersand().serialize(this.component);
    }
}
