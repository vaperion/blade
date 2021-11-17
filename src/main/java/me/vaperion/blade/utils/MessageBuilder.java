package me.vaperion.blade.utils;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class MessageBuilder {

    public static String toStringFormat(BaseComponent[] components) {
        StringBuilder strBuilder = new StringBuilder();

        for (BaseComponent component : components) {
            strBuilder.append(component.toLegacyText());
        }

        return strBuilder.toString();
    }

    private final ComponentBuilder builder;

    public MessageBuilder(String mainText) {
        builder = new ComponentBuilder(mainText);
    }

    public MessageBuilder append(String text) {
        builder.append(text);
        return this;
    }

    public MessageBuilder hover(String[] lines) {
        builder.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(
              combineMultiLine(lines)
        ).create()));
        return this;
    }

    public MessageBuilder hover(List<String> lines) {
        builder.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(
              combineMultiLine(lines)
        ).create()));
        return this;
    }

    public MessageBuilder click(String command, boolean instant) {
        builder.event(new ClickEvent(instant ? ClickEvent.Action.RUN_COMMAND : ClickEvent.Action.SUGGEST_COMMAND, command));
        return this;
    }

    public MessageBuilder reset() {
        builder.reset();
        return this;
    }

    public BaseComponent[] build() {
        return builder.create();
    }

    private String combineMultiLine(String[] lines) {
        return String.join("\n", lines);
    }

    private String combineMultiLine(List<String> lines) {
        return String.join("\n", lines);
    }

    public String toStringFormat() {
        StringBuilder strBuilder = new StringBuilder();

        for (BaseComponent component : builder.create()) {
            strBuilder.append(component.toLegacyText());
        }

        return strBuilder.toString();
    }

    public void sendTo(CommandSender sender) {
        if (sender instanceof Player) {
            ((Player) sender).spigot().sendMessage(build());
            return;
        }

        sender.sendMessage(toStringFormat());
    }
}