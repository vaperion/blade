package me.vaperion.blade.velocity.command;

import com.velocitypowered.api.command.CommandSource;
import me.vaperion.blade.annotation.parameter.Flag;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.command.InternalUsage;
import me.vaperion.blade.command.parameter.DefinedArgument;
import me.vaperion.blade.command.parameter.DefinedFlag;
import me.vaperion.blade.context.Context;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;

public final class VelocityInternalUsage implements InternalUsage<Component> {

    private final Component component;
    private String toString;

    public VelocityInternalUsage(BladeCommand command) {
        this(command, true);
    }

    public VelocityInternalUsage(BladeCommand command, boolean addPrefix) {
        TextComponent.Builder builder = text();

        builder.append(text((addPrefix ? "Usage: " : "") + "/", NamedTextColor.RED)
                .hoverEvent(HoverEvent.showText(text(command.description(), NamedTextColor.GRAY))))
            .append(text(command.mainLabel(), NamedTextColor.RED));

        if (!command.customUsage().isEmpty()) {
            builder.append(text(command.customUsage(), NamedTextColor.RED));
            this.component = builder.build();
            return;
        }

        // Add flag parameters
        boolean first = true;
        for (DefinedFlag definedFlag : command.flags()) {
            Flag flag = definedFlag.flag();

            if (first) {
                builder.append(text(" (", NamedTextColor.RED))
                    .hoverEvent(HoverEvent.showText(text(command.description(), NamedTextColor.GRAY)));
                first = false;
            } else {
                builder.append(text(" | ", NamedTextColor.RED))
                    .hoverEvent(HoverEvent.showText(text(command.description(), NamedTextColor.GRAY)));
            }

            builder
                .append(text("-" + flag.value() + (definedFlag.isBooleanFlag() ? "" : " <" + definedFlag.name() + ">"), NamedTextColor.AQUA))
                .hoverEvent(HoverEvent.showText(text(flag.description(), NamedTextColor.GRAY)));
        }

        if (!first) {
            builder.append(text(")", NamedTextColor.RED))
                .hoverEvent(HoverEvent.showText(text(command.description(), NamedTextColor.GRAY)));
        }

        // Add real parameters
        for (DefinedArgument arg : command.arguments()) {
            builder.append(text(" ", NamedTextColor.RED))
                .append(text(arg.isOptional() ? "(" : "<", NamedTextColor.RED))
                .append(text(arg.name(), NamedTextColor.RED))
                .append(arg.isGreedy() ? text("...", NamedTextColor.RED) : empty())
                .append(text(arg.isOptional() ? ")" : ">", NamedTextColor.RED));
        }

        // Add extra usage
        if (!command.extraUsageData().isEmpty()) {
            builder.append(text(" " + command.extraUsageData().trim(), NamedTextColor.RED));
        }

        this.component = builder.build();
    }

    @Override
    public @NotNull Component message() {
        return this.component;
    }

    @Override
    public void sendTo(@NotNull Context context) {
        ((CommandSource) context.sender().rawSender()).sendMessage(this.component);
    }

    @NotNull
    @Override
    public String toString() {
        if (toString == null) {
            toString = LegacyComponentSerializer.legacyAmpersand().serialize(component);
        }

        return toString;
    }
}
