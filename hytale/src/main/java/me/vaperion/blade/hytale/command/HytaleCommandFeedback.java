package me.vaperion.blade.hytale.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import me.vaperion.blade.annotation.parameter.Flag;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.command.CommandFeedback;
import me.vaperion.blade.command.parameter.DefinedArgument;
import me.vaperion.blade.command.parameter.DefinedFlag;
import me.vaperion.blade.context.Context;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

import static com.hypixel.hytale.server.core.Message.raw;

public final class HytaleCommandFeedback implements CommandFeedback<Message> {

    private final Message message;

    public HytaleCommandFeedback(@NotNull BladeCommand command, boolean isUsage) {
        Message message = Message.empty();

        message.insert(
            raw(
                (isUsage ? "Usage: " : "") + "/" + command.mainLabel()
            ).color(Color.RED)
        );

        if (!command.customUsage().isEmpty()) {
            this.message = message.insert(
                raw(command.customUsage()).color(Color.RED)
            );
            return;
        }

        // Add flag parameters
        boolean first = true;
        for (DefinedFlag definedFlag : command.flags()) {
            Flag flag = definedFlag.flag();

            if (first) {
                message.insert(
                    raw(" (").color(Color.RED)
                );
                first = false;
            } else {
                message.insert(
                    raw(" | ").color(Color.RED)
                );
            }

            message.insert(
                raw(
                    "-" + flag.value() + (definedFlag.isBooleanFlag() ? "" : " <" + definedFlag.name() + ">")
                ).color(Color.CYAN)
            );
        }

        if (!first) {
            message.insert(
                raw(")").color(Color.RED)
            );
        }

        // Add real parameters
        for (DefinedArgument arg : command.arguments()) {
            message.insert(
                raw(
                    " " +
                        (arg.isOptional() ? "[" : "<") +
                        arg.name() +
                        (arg.isGreedy() ? "..." : "") +
                        (arg.isOptional() ? "]" : ">")
                ).color(Color.RED)
            );
        }

        // Add extra usage
        if (!command.extraUsageData().isEmpty()) {
            message.insert(
                raw(" " + command.extraUsageData()).color(Color.RED)
            );
        }

        this.message = message;
    }

    @Override
    public @NotNull Message message() {
        return this.message;
    }

    @Override
    public void sendTo(@NotNull Context context) {
        ((CommandSender) context.sender().rawSender()).sendMessage(this.message);
    }
}
