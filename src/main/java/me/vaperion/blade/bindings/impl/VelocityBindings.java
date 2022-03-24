package me.vaperion.blade.bindings.impl;

import com.velocitypowered.api.proxy.Player;
import me.vaperion.blade.bindings.Binding;
import me.vaperion.blade.bindings.impl.provider.VelocityPlayerBladeProvider;
import me.vaperion.blade.service.BladeCommandService;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

public class VelocityBindings implements Binding {

    public static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[34][0-9a-fA-F]{3}-[89ab][0-9a-fA-F]{3}-[0-9a-fA-F]{12}");

    @Override
    public void bind(@NotNull BladeCommandService commandService) {
        commandService.bindProvider(Player.class, new VelocityPlayerBladeProvider());
    }
}
