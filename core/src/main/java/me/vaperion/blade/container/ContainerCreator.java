package me.vaperion.blade.container;

import me.vaperion.blade.Blade;
import org.jetbrains.annotations.NotNull;

public interface ContainerCreator<T extends Container> {
    /**
     * Creates a new container instance.
     *
     * @param blade the blade instance
     * @param label the label used to execute the command
     * @return the created container instance
     */
    @NotNull
    T create(@NotNull Blade blade, @NotNull String label) throws Exception;

    @SuppressWarnings("unused")
    class NoOp implements ContainerCreator<NoOp.NoOpContainer> {
        public static final ContainerCreator<NoOpContainer> CREATOR = NoOpContainer::new;

        @Override
        public @NotNull NoOpContainer create(@NotNull Blade blade,
                                             @NotNull String label) {
            return new NoOpContainer(blade, label);
        }

        public static class NoOpContainer implements Container {
            private final Blade blade;

            protected NoOpContainer(@NotNull Blade blade,
                                    @NotNull String label) {
                this.blade = blade;
            }

            @Override
            public @NotNull Blade blade() {
                return this.blade;
            }

            @Override
            public void unregister() {
                // No-op
            }
        }
    }
}
