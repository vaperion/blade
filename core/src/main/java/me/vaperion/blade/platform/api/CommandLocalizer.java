package me.vaperion.blade.platform.api;

import me.vaperion.blade.annotation.command.Description;
import me.vaperion.blade.annotation.parameter.Flag;
import me.vaperion.blade.annotation.parameter.Name;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.command.BladeParameter;
import me.vaperion.blade.command.parameter.DefinedFlag;
import me.vaperion.blade.context.Sender;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Localizes user-facing command messages for a specific sender.
 * <p>
 * The values set in annotations such as {@link Description}, {@link Name}
 * and {@link Flag} are passed to each method as the {@code fallback} argument.
 * <p>
 * Every method is invoked even when the fallback is empty (for example when a
 * command has no {@link Description} annotation).
 * <p>
 * Rendered usage and help messages are cached per {@link #localeOf(Sender)}
 * result.
 * <p>
 * Returning an empty string suppresses the corresponding section in the
 * rendered output.
 */
public interface CommandLocalizer {

    /**
     * Resolves the locale used to localize messages for the given sender.
     * The result is used as the cache key.
     *
     * @param sender the sender to resolve the locale for
     * @return the sender's locale
     */
    @NotNull
    default Locale localeOf(@NotNull Sender<?> sender) {
        return Locale.getDefault();
    }

    /**
     * Localizes the description of a command.
     *
     * @param sender   the command sender
     * @param command  the command
     * @param fallback the untranslated description
     * @return the localized description, or an empty string to hide it
     */
    @NotNull
    default String commandDescription(@NotNull Sender<?> sender,
                                      @NotNull BladeCommand command,
                                      @NotNull String fallback) {
        return fallback;
    }

    /**
     * Localizes the display name of a parameter (argument or flag value).
     *
     * @param sender    the command sender
     * @param command   the command
     * @param parameter the parameter
     * @param fallback  the untranslated name
     * @return the localized parameter name
     */
    @NotNull
    default String parameterName(@NotNull Sender<?> sender,
                                 @NotNull BladeCommand command,
                                 @NotNull BladeParameter parameter,
                                 @NotNull String fallback) {
        return fallback;
    }

    /**
     * Localizes the description of a parameter.
     *
     * @param sender    the command sender
     * @param command   the command
     * @param parameter the parameter
     * @param fallback  the untranslated description
     * @return the localized description, or an empty string to hide it
     */
    @NotNull
    default String parameterDescription(@NotNull Sender<?> sender,
                                        @NotNull BladeCommand command,
                                        @NotNull BladeParameter parameter,
                                        @NotNull String fallback) {
        return fallback;
    }

    /**
     * Localizes the description of a flag.
     *
     * @param sender   the command sender
     * @param command  the command
     * @param flag     the flag
     * @param fallback the untranslated description
     * @return the localized description, or an empty string to hide it
     */
    @NotNull
    default String flagDescription(@NotNull Sender<?> sender,
                                   @NotNull BladeCommand command,
                                   @NotNull DefinedFlag flag,
                                   @NotNull String fallback) {
        return fallback;
    }

    /**
     * Localizes the custom usage string of a command.
     *
     * @param sender   the command sender
     * @param command  the command
     * @param fallback the untranslated usage string
     * @return the localized usage string, or an empty string to hide it
     */
    @NotNull
    default String customUsage(@NotNull Sender<?> sender,
                               @NotNull BladeCommand command,
                               @NotNull String fallback) {
        return fallback;
    }

    /**
     * Localizes the extra usage data of a command.
     *
     * @param sender   the command sender
     * @param command  the command
     * @param fallback the untranslated extra usage data
     * @return the localized extra usage data, or an empty string to hide it
     */
    @NotNull
    default String extraUsage(@NotNull Sender<?> sender,
                              @NotNull BladeCommand command,
                              @NotNull String fallback) {
        return fallback;
    }

    /**
     * The default localizer.
     */
    final class Default implements CommandLocalizer {
    }

}
