package me.vaperion.blade.test.container;

import lombok.Getter;
import me.vaperion.blade.Blade;
import me.vaperion.blade.container.Container;
import me.vaperion.blade.container.ContainerCreator;
import org.jetbrains.annotations.NotNull;

@Getter
public final class TestContainer implements Container {

    public static final ContainerCreator<TestContainer> CREATOR = TestContainer::new;

    private final Blade blade;
    private final String label;

    public TestContainer(@NotNull Blade blade,
                         @NotNull String label) {
        this.blade = blade;
        this.label = label;
    }
}
