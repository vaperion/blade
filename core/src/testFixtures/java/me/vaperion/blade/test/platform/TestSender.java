package me.vaperion.blade.test.platform;

import me.vaperion.blade.command.BladeCommand;
import me.vaperion.blade.context.Sender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TestSender implements Sender<TestCommandSource> {

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
