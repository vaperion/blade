package me.vaperion.blade.velocity.command;

import com.velocitypowered.api.command.CommandSource;
import me.vaperion.blade.annotation.argument.Flag;
import me.vaperion.blade.command.Command;
import me.vaperion.blade.command.Parameter.CommandParameter;
import me.vaperion.blade.command.Parameter.FlagParameter;
import me.vaperion.blade.command.UsageMessage;
import me.vaperion.blade.context.Context;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;

public final class VelocityUsageMessage implements UsageMessage {

    private final Component component;
    private String toString;

    public VelocityUsageMessage(Command command) {
        this(command, true);
    }

    public VelocityUsageMessage(Command command, boolean addPrefix) {
        TextComponent.Builder builder = text();

        builder.append(text((addPrefix ? "Usage: " : "") + "/", NamedTextColor.RED)
                    .hoverEvent(HoverEvent.showText(text(command.getDescription(), NamedTextColor.GRAY))))
              .append(text(command.getUsageAlias(), NamedTextColor.RED));

        if (!command.getCustomUsage().isEmpty()) {
            builder.append(text(command.getCustomUsage(), NamedTextColor.RED));
            this.component = builder.build();
            return;
        }

        // Add flag parameters
        boolean first = true;
        for (FlagParameter flagParameter : command.getFlagParameters()) {
            Flag flag = flagParameter.getFlag();

            if (first) {
                builder.append(text(" (", NamedTextColor.RED));
                first = false;
            } else {
                builder.append(text(" | ", NamedTextColor.RED))
                      .hoverEvent(HoverEvent.showText(text(command.getDescription(), NamedTextColor.GRAY)));
            }

            builder
                  .append(text("-" + flag.value() + (flagParameter.isBooleanFlag() ? "" : " <" + flagParameter.getName() + ">"), NamedTextColor.AQUA))
                  .hoverEvent(HoverEvent.showText(text(flag.description(), NamedTextColor.GRAY)));
        }
        if (!first) builder.append(text(")", NamedTextColor.RED))
              .hoverEvent(HoverEvent.showText(text(command.getDescription(), NamedTextColor.GRAY)));

        // Add real parameters
        for (CommandParameter commandParameter : command.getCommandParameters()) {
            builder.append(text(" ", NamedTextColor.RED))
                  .append(text(commandParameter.isOptional() ? "(" : "<", NamedTextColor.RED))
                  .append(text(commandParameter.getName(), NamedTextColor.RED))
                  .append(commandParameter.isText() ? text("...", NamedTextColor.RED) : empty())
                  .append(text(commandParameter.isOptional() ? ")" : ">", NamedTextColor.RED));
        }

        // Add extra usage
        if (!command.getExtraUsageData().isEmpty()) {
            builder.append(text(" " + command.getExtraUsageData().trim(), NamedTextColor.RED));
        }

        this.component = builder.build();
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
