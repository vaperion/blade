package me.vaperion.blade;

import me.vaperion.blade.annotation.command.Command;
import me.vaperion.blade.annotation.command.Description;
import me.vaperion.blade.annotation.parameter.Flag;
import me.vaperion.blade.annotation.parameter.Name;
import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.command.BladeParameter;
import me.vaperion.blade.command.CommandFeedback;
import me.vaperion.blade.context.Context;
import me.vaperion.blade.context.Sender;
import me.vaperion.blade.platform.api.CommandLocalizer;
import me.vaperion.blade.test.BladeTestPlatform;
import me.vaperion.blade.test.platform.TestCommandSource;
import me.vaperion.blade.util.BladeHelper;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Locale;

public class LocalizationTest {

    public static class Commands {
        @Command("ban")
        @Description("Bans a player")
        public static void ban(@Name("target") @Description("The player to ban") String target,
                               @Flag(value = 's', description = "Ban silently") boolean silent) {
        }
    }

    @Test
    public void defaultLocalizerSharesLegacyCache() {
        Blade blade = BladeTestPlatform.createInstance();
        blade.register(Commands.class);

        BladeCommand command = blade.commands().get(0);
        Context context = contextFor(blade, Locale.US);

        Assertions.assertSame(command.usageMessage(), command.usageMessage(context));
        Assertions.assertSame(command.helpMessage(), command.helpMessage(context));
    }

    @Test
    public void localizesAndCachesPerLocale() {
        Blade blade = BladeTestPlatform.createInstance();
        blade.configuration().localizer(new GermanLocalizer());
        blade.register(Commands.class);

        BladeCommand command = blade.commands().get(0);

        Context german = contextFor(blade, Locale.GERMAN);
        Context english = contextFor(blade, Locale.US);

        String germanUsage = BladeHelper.plainText(command.usageMessage(german).message());
        String englishUsage = BladeHelper.plainText(command.usageMessage(english).message());

        Assertions.assertEquals("Usage: /ban (-s) <spieler>", germanUsage);
        Assertions.assertEquals("Usage: /ban (-s) <target>", englishUsage);

        CommandFeedback cachedGerman = command.usageMessage(contextFor(blade, Locale.GERMAN));
        Assertions.assertSame(command.usageMessage(german), cachedGerman);
        Assertions.assertNotSame(command.usageMessage(german), command.usageMessage(english));
    }

    @Test
    public void parameterDescriptionIsReadFromAnnotation() {
        Blade blade = BladeTestPlatform.createInstance();
        blade.register(Commands.class);

        BladeParameter target = blade.commands().get(0).arguments().get(0);
        Assertions.assertEquals("The player to ban", target.description());
    }

    private static final class GermanLocalizer implements CommandLocalizer {
        @Override
        public @NotNull Locale localeOf(@NotNull Sender<?> sender) {
            return ((LocaleSender) sender).locale;
        }

        @Override
        public @NotNull String parameterName(@NotNull Sender<?> sender,
                                             @NotNull BladeCommand command,
                                             @NotNull BladeParameter parameter,
                                             @NotNull String fallback) {
            if (localeOf(sender).equals(Locale.GERMAN) && "target".equals(fallback)) {
                return "spieler";
            }

            return fallback;
        }
    }

    @NotNull
    private static Context contextFor(@NotNull Blade blade, @NotNull Locale locale) {
        return new Context(blade, new LocaleSender(locale), "ban", new String[0]);
    }

    private static final class LocaleSender implements Sender<TestCommandSource> {
        private final Locale locale;

        private LocaleSender(@NotNull Locale locale) {
            this.locale = locale;
        }

        @Override
        public void sendMessage(@NotNull Component component) {
        }

        @Override
        public @NotNull TestCommandSource rawSender() {
            return TestCommandSource.TEST;
        }

        @Override
        public @NotNull Object underlyingSender() {
            return TestCommandSource.TEST;
        }

        @Override
        public @NotNull Class<?> underlyingSenderType() {
            return TestCommandSource.class;
        }

        @Override
        public @NotNull String name() {
            return TestCommandSource.TEST.name();
        }

        @Override
        public boolean hasPermission(@NotNull String permission) {
            return true;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <S> @Nullable S parseAs(@NotNull Class<S> clazz) {
            if (clazz == TestCommandSource.class)
                return (S) TestCommandSource.TEST;

            return null;
        }

        @Override
        public boolean isExpectedType(@NotNull BladeCommand command) {
            return parseAs(command.senderType()) != null;
        }
    }
}
