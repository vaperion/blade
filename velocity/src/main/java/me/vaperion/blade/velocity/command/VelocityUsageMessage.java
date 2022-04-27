package me.vaperion.blade.velocity.command;

import com.velocitypowered.api.command.CommandSource;
import me.vaperion.blade.annotation.argument.Flag;
import me.vaperion.blade.command.Command;
import me.vaperion.blade.command.Parameter.CommandParameter;
import me.vaperion.blade.command.Parameter.FlagParameter;
import me.vaperion.blade.command.UsageMessage;
import me.vaperion.blade.context.Context;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

public final class VelocityUsageMessage implements UsageMessage {

    private final Component component;
    private String toString;

    public VelocityUsageMessage(Command command) {
        this(command, true);
    }

    public VelocityUsageMessage(Command command, boolean addPrefix) {
        Component component = Component.text((addPrefix ? "Usage: " : "") + "/").color(NamedTextColor.RED)
              .hoverEvent(HoverEvent.showText(
                    Component.text(command.getDescription()).color(NamedTextColor.GRAY))
              ).append(
                    Component.text(command.getUsageAlias())
              );

        if (!command.getCustomUsage().isEmpty()) {
            this.component = component.append(
                  Component.text(command.getCustomUsage())
            );
            return;
        }

        // Add flag parameters
        boolean first = true;
        for (FlagParameter flagParameter : command.getFlagParameters()) {
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
            if (commandParameter.isText()) component = component.append(
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
    public void sendTo(@NotNull Context context) {
        ((CommandSource) context.sender().getSender()).sendMessage(this.component);
    }

    @NotNull
    @Override
    public String toString() {
        if (toString == null) toString = LegacyComponentSerializer.legacyAmpersand().serialize(component);
        return toString;
    }
}
