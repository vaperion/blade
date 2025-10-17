package me.vaperion.blade.annotation.command;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method or class as a command.
 *
 * <h3>Usage Patterns:</h3>
 * <ul>
 *   <li><strong>Method-level:</strong> Registers the method as a standalone command</li>
 *   <li><strong>Class-level:</strong> Acts as a prefix for all command methods within the class</li>
 * </ul>
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * @Command("mycommand")
 * public class MyCommandClass {
 *     @Command("subcommand")
 *     public static void mySubCommand(@Sender Player player) {
 *         player.sendMessage("This is a subcommand!");
 *     }
 * }
 * }</pre>
 *
 * <p>In this example the command registers as: {@code /mycommand subcommand}</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface Command {
    /**
     * The command labels.
     * <p>
     * The first label is used as the primary name in generated help/usage messages.
     *
     * @return array of command labels
     */
    @NotNull
    String[] value();
}