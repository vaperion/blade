package me.vaperion.blade.fabric.mixin;

import me.vaperion.blade.fabric.ext.CommandSourceStackExt;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(CommandSourceStack.class)
public final class CommandSourceStackMixin implements CommandSourceStackExt {

    @Shadow
    @Final
    private CommandSource source;

    @Override
    public boolean blade$isConsole() {
        return this.source instanceof MinecraftServer;
    }
}
