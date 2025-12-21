package me.vaperion.blade.fabric.command;

import me.vaperion.blade.annotation.parameter.Flag;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.command.CommandFeedback;
import me.vaperion.blade.command.parameter.DefinedArgument;
import me.vaperion.blade.command.parameter.DefinedFlag;
import me.vaperion.blade.context.Context;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

public final class FabricCommandFeedback implements CommandFeedback<Text> {

    private final Text component;

    public FabricCommandFeedback(BladeCommand command, boolean isUsage) {
        MutableText builder = Text.empty();

        builder.append(
            Text.literal((isUsage ? "Usage: " : "") + "/")
                .styled(style -> style
                    .withColor(Formatting.RED))
        );

        builder.append(
            Text.literal(command.mainLabel()).formatted(Formatting.RED)
        );

        if (!command.customUsage().isEmpty()) {
            builder.append(
                Text.literal(command.customUsage()).formatted(Formatting.RED)
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
                    Text.literal(" (").styled(style -> style
                        .withFormatting(Formatting.RED))
                );

                first = false;
            } else {
                builder.append(
                    Text.literal(" | ").styled(style -> style
                        .withFormatting(Formatting.RED))
                );
            }

            builder.append(
                Text.literal("-" + flag.value() + (definedFlag.isBooleanFlag() ? "" : " <" + definedFlag.name() + ">"))
                    .styled(style -> style
                        .withColor(Formatting.AQUA))
            );
        }

        if (!first) {
            builder.append(
                Text.literal(")").styled(style -> style
                    .withFormatting(Formatting.RED))
            );
        }

        // Add real parameters
        for (DefinedArgument arg : command.arguments()) {
            builder.append(Text.literal(" "));

            builder.append(
                Text.literal(arg.isOptional() ? "[" : "<")
                    .formatted(Formatting.RED)
            );

            builder.append(
                Text.literal(arg.name())
                    .formatted(Formatting.RED)
            );

            if (arg.isGreedy()) {
                builder.append(
                    Text.literal("...").formatted(Formatting.RED)
                );
            }

            builder.append(
                Text.literal(arg.isOptional() ? "]" : ">")
                    .formatted(Formatting.RED)
            );
        }

        // Add extra usage
        if (!command.extraUsageData().isEmpty()) {
            builder.append(
                Text.literal(" " + command.extraUsageData().trim())
                    .formatted(Formatting.RED)
            );
        }

        this.component = builder;
    }

    @Override
    public @NotNull Text message() {
        return component;
    }

    @Override
    public void sendTo(@NotNull Context context) {
        ((ServerCommandSource) context.sender().rawSender()).sendMessage(this.component);
    }
}
