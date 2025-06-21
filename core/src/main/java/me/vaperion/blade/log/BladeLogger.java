package me.vaperion.blade.log;

import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public interface BladeLogger {

    /**
     * Default logger instance.
     */
    @NotNull
    BladeLogger DEFAULT = new Default();

    void info(@NotNull String msg, @NotNull Object... args);

    void warn(@NotNull String msg, @NotNull Object... args);

    void error(@NotNull String msg, @NotNull Object... args);

    void error(@NotNull Throwable throwable, @NotNull String msg, @NotNull Object... args);

    class Default implements BladeLogger {
        @Override
        public void info(@NotNull String msg, @NotNull Object... args) {
            System.out.printf("[Blade/INFO]: " + msg + "%n", args);
        }

        @Override
        public void warn(@NotNull String msg, @NotNull Object... args) {
            System.out.printf("[Blade/WARN]: " + msg + "%n", args);
        }

        @Override
        public void error(@NotNull String msg, @NotNull Object... args) {
            System.err.printf("[Blade/ERROR]: " + msg + "%n", args);
        }

        @Override
        public void error(@NotNull Throwable throwable, @NotNull String msg, @NotNull Object... args) {
            System.err.printf("[Blade/ERROR]: " + msg + "%n", args);
            throwable.printStackTrace(System.err);
        }
    }

}
