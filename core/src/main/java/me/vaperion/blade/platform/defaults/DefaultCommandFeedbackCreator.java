package me.vaperion.blade.platform.defaults;

import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.command.CommandFeedback;
import me.vaperion.blade.command.parameter.DefinedArgument;
import me.vaperion.blade.command.parameter.DefinedFlag;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.context.Sender;
import me.vaperion.blade.platform.api.CommandFeedbackCreator;
import me.vaperion.blade.platform.api.CommandLocalizer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;

public class DefaultCommandFeedbackCreator implements CommandFeedbackCreator {

    @Override
    public @NotNull CommandFeedback create(@NotNull BladeCommand command, boolean isUsage) {
        return new ComponentFeedback(build(command, isUsage));
    }

    @Override
    public @NotNull CommandFeedback create(@NotNull BladeCommand command,
                                           boolean isUsage,
                                           @NotNull Context context) {
        return new ComponentFeedback(build(command, isUsage, context));
    }

    @NotNull
    protected Component build(@NotNull BladeCommand command, boolean isUsage) {
        return build(command, isUsage, null);
    }

    @NotNull
    protected Component build(@NotNull BladeCommand command,
                              boolean isUsage,
                              @Nullable Context context) {
        String descriptionText = localize(context, command.description(),
            (localizer, sender, fallback) -> localizer.commandDescription(sender, command, fallback));

        HoverEvent<Component> descriptionHover = descriptionText.isEmpty()
            ? null
            : HoverEvent.showText(text(descriptionText, NamedTextColor.GRAY));

        TextComponent.Builder builder = text();

        builder.append(text((isUsage ? "Usage: " : "") + "/", NamedTextColor.RED)
                .hoverEvent(descriptionHover))
            .append(text(command.mainLabel(), NamedTextColor.RED));

        String customUsage = localize(context, command.customUsage(),
            (localizer, sender, fallback) -> localizer.customUsage(sender, command, fallback));

        if (!customUsage.isEmpty()) {
            builder.append(text(" " + customUsage, NamedTextColor.RED));
            return builder.asComponent();
        }

        // Add flag parameters
        boolean first = true;
        for (DefinedFlag definedFlag : command.flags()) {
            if (first) {
                builder.append(text(" (", NamedTextColor.RED)
                    .hoverEvent(descriptionHover));
                first = false;
            } else {
                builder.append(text(" | ", NamedTextColor.RED)
                    .hoverEvent(descriptionHover));
            }

            String flagDescriptionFallback = !definedFlag.flag().description().isEmpty()
                ? definedFlag.flag().description()
                : definedFlag.description();

            String flagName = localize(context, definedFlag.name(),
                (localizer, sender, fallback) -> localizer.parameterName(sender, command, definedFlag, fallback));
            String flagDescription = localize(context, flagDescriptionFallback,
                (localizer, sender, fallback) -> localizer.flagDescription(sender, command, definedFlag, fallback));

            HoverEvent<Component> flagHover = flagDescription.isEmpty()
                ? null
                : HoverEvent.showText(text(flagDescription, NamedTextColor.GRAY));

            builder.append(text("-" + definedFlag.getChar() + (definedFlag.isBooleanFlag() ? "" : " <" + flagName + ">"), NamedTextColor.AQUA)
                .hoverEvent(flagHover));
        }

        if (!first) {
            builder.append(text(")", NamedTextColor.RED)
                .hoverEvent(descriptionHover));
        }

        // Add real parameters
        for (DefinedArgument arg : command.arguments()) {
            String argName = localize(context, arg.name(),
                (localizer, sender, fallback) -> localizer.parameterName(sender, command, arg, fallback));
            String argDescription = localize(context, arg.description(),
                (localizer, sender, fallback) -> localizer.parameterDescription(sender, command, arg, fallback));

            HoverEvent<Component> argHover = argDescription.isEmpty()
                ? null
                : HoverEvent.showText(text(argDescription, NamedTextColor.GRAY));

            builder.append(text(" ", NamedTextColor.RED))
                .append(text(arg.isOptional() ? "[" : "<", NamedTextColor.RED).hoverEvent(argHover))
                .append(text(argName, NamedTextColor.RED).hoverEvent(argHover))
                .append(arg.isGreedy() ? text("...", NamedTextColor.RED).hoverEvent(argHover) : empty())
                .append(text(arg.isOptional() ? "]" : ">", NamedTextColor.RED).hoverEvent(argHover));
        }

        // Add extra usage
        String extraUsage = localize(context, command.extraUsageData(),
            (localizer, sender, fallback) -> localizer.extraUsage(sender, command, fallback));

        if (!extraUsage.isEmpty()) {
            builder.append(text(" " + extraUsage.trim(), NamedTextColor.RED));
        }

        return builder.asComponent();
    }

    @NotNull
    private static String localize(@Nullable Context context,
                                   @NotNull String fallback,
                                   @NotNull LocalizationCall call) {
        if (context == null) {
            return fallback;
        }

        CommandLocalizer localizer = context.blade().configuration().localizer();
        return call.localize(localizer, context.sender(), fallback);
    }

    @FunctionalInterface
    private interface LocalizationCall {
        @NotNull
        String localize(@NotNull CommandLocalizer localizer,
                        @NotNull Sender<?> sender,
                        @NotNull String fallback);
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
