package me.vaperion.blade.fabric.command;

import me.vaperion.blade.annotation.parameter.Flag;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.command.CommandFeedback;
import me.vaperion.blade.command.parameter.DefinedArgument;
import me.vaperion.blade.command.parameter.DefinedFlag;
import me.vaperion.blade.context.Context;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;

public final class FabricCommandFeedback implements CommandFeedback<Component> {

    private final Component component;

    public FabricCommandFeedback(BladeCommand command, boolean isUsage) {
        MutableComponent builder = Component.empty();

        builder.append(
            Component.literal((isUsage ? "Usage: " : "") + "/")
                .withStyle(style -> style
                    .withColor(ChatFormatting.RED))
        );

        builder.append(
            Component.literal(command.mainLabel()).withStyle(ChatFormatting.RED)
        );

        if (!command.customUsage().isEmpty()) {
            builder.append(
                Component.literal(command.customUsage()).withStyle(ChatFormatting.RED)
            );
            this.component = builder;
            return;
        }

        // Add flag parameters
        boolean first = true;
        for (DefinedFlag definedFlag : command.flags()) {
            Flag flag = definedFlag.flag();

            if (first) {
                builder.append(
                    Component.literal(" (").withStyle(style -> style
                        .applyFormat(ChatFormatting.RED))
                );

                first = false;
            } else {
                builder.append(
                    Component.literal(" | ").withStyle(style -> style
                        .applyFormat(ChatFormatting.RED))
                );
            }

            builder.append(
                Component.literal("-" + flag.value() + (definedFlag.isBooleanFlag() ? "" : " <" + definedFlag.name() + ">"))
                    .withStyle(style -> style
                        .withColor(ChatFormatting.AQUA))
            );
        }

        if (!first) {
            builder.append(
                Component.literal(")").withStyle(style -> style
                    .applyFormat(ChatFormatting.RED))
            );
        }

        // Add real parameters
        for (DefinedArgument arg : command.arguments()) {
            builder.append(Component.literal(" "));

            builder.append(
                Component.literal(arg.isOptional() ? "[" : "<")
                    .withStyle(ChatFormatting.RED)
            );

            builder.append(
                Component.literal(arg.name())
                    .withStyle(ChatFormatting.RED)
            );

            if (arg.isGreedy()) {
                builder.append(
                    Component.literal("...").withStyle(ChatFormatting.RED)
                );
            }

            builder.append(
                Component.literal(arg.isOptional() ? "]" : ">")
                    .withStyle(ChatFormatting.RED)
            );
        }

        // Add extra usage
        if (!command.extraUsageData().isEmpty()) {
            builder.append(
                Component.literal(" " + command.extraUsageData().trim())
                    .withStyle(ChatFormatting.RED)
            );
        }

        this.component = builder;
    }

    @Override
    public @NotNull Component message() {
        return component;
    }

    @Override
    public void sendTo(@NotNull Context context) {
        ((CommandSourceStack) context.sender().rawSender()).sendSystemMessage(this.component);
    }
}
