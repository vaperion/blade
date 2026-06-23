package me.vaperion.blade.platform.defaults;

import me.vaperion.blade.annotation.parameter.Flag;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.command.CommandFeedback;
import me.vaperion.blade.command.parameter.DefinedArgument;
import me.vaperion.blade.command.parameter.DefinedFlag;
import me.vaperion.blade.platform.api.CommandFeedbackCreator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;

public class DefaultCommandFeedbackCreator implements CommandFeedbackCreator {

    @Override
    public @NotNull CommandFeedback create(@NotNull BladeCommand command, boolean isUsage) {
        return new ComponentFeedback(build(command, isUsage));
    }

    @NotNull
    protected Component build(@NotNull BladeCommand command, boolean isUsage) {
        Component description = text(command.description(), NamedTextColor.GRAY);

        TextComponent.Builder builder = text();

        builder.append(text((isUsage ? "Usage: " : "") + "/", NamedTextColor.RED)
                .hoverEvent(HoverEvent.showText(description)))
            .append(text(command.mainLabel(), NamedTextColor.RED));

        if (!command.customUsage().isEmpty()) {
            builder.append(text(" " + command.customUsage(), NamedTextColor.RED));
            return builder.asComponent();
        }

        // Add flag parameters
        boolean first = true;
        for (DefinedFlag definedFlag : command.flags()) {
            Flag flag = definedFlag.flag();

            if (first) {
                builder.append(text(" (", NamedTextColor.RED)
                    .hoverEvent(HoverEvent.showText(description)));
                first = false;
            } else {
                builder.append(text(" | ", NamedTextColor.RED)
                    .hoverEvent(HoverEvent.showText(description)));
            }

            builder.append(text("-" + flag.value() + (definedFlag.isBooleanFlag() ? "" : " <" + definedFlag.name() + ">"), NamedTextColor.AQUA)
                .hoverEvent(HoverEvent.showText(text(flag.description(), NamedTextColor.GRAY))));
        }

        if (!first) {
            builder.append(text(")", NamedTextColor.RED)
                .hoverEvent(HoverEvent.showText(description)));
        }

        // Add real parameters
        for (DefinedArgument arg : command.arguments()) {
            builder.append(text(" ", NamedTextColor.RED))
                .append(text(arg.isOptional() ? "[" : "<", NamedTextColor.RED))
                .append(text(arg.name(), NamedTextColor.RED))
                .append(arg.isGreedy() ? text("...", NamedTextColor.RED) : empty())
                .append(text(arg.isOptional() ? "]" : ">", NamedTextColor.RED));
        }

        // Add extra usage
        if (!command.extraUsageData().isEmpty()) {
            builder.append(text(" " + command.extraUsageData().trim(), NamedTextColor.RED));
        }

        return builder.asComponent();
    }

    public static final class ComponentFeedback implements CommandFeedback {
        private final Component component;

        public ComponentFeedback(@NotNull Component component) {
            this.component = component;
        }

        @Override
        public @NotNull Component message() {
            return component;
        }
    }
}
