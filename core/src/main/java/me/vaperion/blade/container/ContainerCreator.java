package me.vaperion.blade.container;

import me.vaperion.blade.Blade;
import me.vaperion.blade.command.BladeCommand;
import org.jetbrains.annotations.NotNull;

public interface ContainerCreator<T extends Container> {
    /**
     * Creates a new container instance.
     *
     * @param blade   the blade instance
     * @param command the base command
     * @param label   the label used to execute the command
     * @return the created container instance
     */
    @NotNull
    T create(@NotNull Blade blade,
             @NotNull BladeCommand command,
             @NotNull String label) throws Exception;

    @SuppressWarnings("unused")
    class NoOp implements ContainerCreator<NoOp.NoOpContainer> {
        public static final ContainerCreator<NoOpContainer> CREATOR = NoOpContainer::new;

        @Override
        public @NotNull NoOpContainer create(@NotNull Blade blade,
                                             @NotNull BladeCommand command,
                                             @NotNull String label) {
            return new NoOpContainer(blade, command, label);
        }

        public static class NoOpContainer implements Container {
            private final Blade blade;
            private final BladeCommand command;

            protected NoOpContainer(@NotNull Blade blade,
                                    @NotNull BladeCommand command,
                                    @NotNull String label) {
                this.blade = blade;
                this.command = command;
            }

            @Override
            public @NotNull Blade blade() {
                return this.blade;
            }

            @Override
            public @NotNull BladeCommand baseCommand() {
                return this.command;
            }
        }
    }
}
