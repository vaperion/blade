package me.vaperion.blade.util.command;

import me.vaperion.blade.Blade;
import me.vaperion.blade.command.BladeCommand;
import org.jetbrains.annotations.NotNull;

public interface CommandExecutionWrapper {

    static void runAsync(@NotNull Blade blade,
                         @NotNull BladeCommand command,
                         @NotNull Runnable runnable) {
        blade.configuration().asyncExecutor().accept(() -> {
            try {
                runnable.run();
            } catch (Throwable t) {
                blade.logger().error(
                    t,
                    "An error occurred while invoking command `%s` (%s#%s) asynchronously.",
                    command.mainLabel(),
                    command.method().getDeclaringClass().getName(),
                    command.method().getName()
                );
            }
        });
    }

    static void runSync(@NotNull Blade blade,
                        @NotNull BladeCommand command,
                        @NotNull Runnable runnable) {
        long start = System.nanoTime();

        try {
            runnable.run();
        } catch (Throwable t) {
            blade.logger().error(
                t,
                "An error occurred while invoking command `%s` (%s#%s) synchronously.",
                command.mainLabel(),
                command.method().getDeclaringClass().getName(),
                command.method().getName()
            );
        } finally {
            long elapsed = (System.nanoTime() - start) / 1_000_000L;

            if (elapsed >= blade.configuration().executionTimeWarningThreshold()) {
                blade.logger().warn(
                    "Invoking command `%s` (%s#%s) synchronously took %d ms (threshold: %d ms).",
                    command.mainLabel(),
                    command.method().getDeclaringClass().getName(),
                    command.method().getName(),
                    elapsed,
                    blade.configuration().executionTimeWarningThreshold()
                );
            }
        }
    }

}
