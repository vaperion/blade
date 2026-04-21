package me.vaperion.blade.brigadier;

import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

@SuppressWarnings("JavaReflectionMemberAccess")
public final class BrigadierCompat {

    private static volatile Field CLIENT_NODE_FIELD;
    private static volatile Field UNWRAPPED_CACHED_FIELD;

    private static boolean CLIENT_NODE_SUPPORTED = true;
    private static boolean UNWRAPPED_CACHED_SUPPORTED = true;

    private BrigadierCompat() {
    }

    public static <T> void setClientNode(@NotNull CommandNode<T> node,
                                         @NotNull LiteralCommandNode<T> clientNode) {
        // some platforms like Paper have a separate client node field that lets us send a different brigadier tree to the client

        if (!CLIENT_NODE_SUPPORTED) {
            return;
        }

        try {
            Field field = clientNodeField();

            field.set(node, clientNode);
        } catch (Throwable ignored) {
            CLIENT_NODE_SUPPORTED = false;
        }
    }

    @Nullable
    public static <T> CommandNode<T> getClientNode(@NotNull CommandNode<T> node) {
        // some platforms like Paper have a separate client node field that lets us send a different brigadier tree to the client

        if (!CLIENT_NODE_SUPPORTED) {
            return null;
        }

        try {
            Field field = clientNodeField();

            //noinspection unchecked
            return (CommandNode<T>) field.get(node);
        } catch (Throwable ignored) {
            CLIENT_NODE_SUPPORTED = false;
            return null;
        }
    }

    @Nullable
    public static <T> CommandNode<T> getUnwrappedCached(@NotNull CommandNode<T> node) {
        if (!UNWRAPPED_CACHED_SUPPORTED) {
            return null;
        }

        try {
            Field field = unwrappedCachedField();

            //noinspection unchecked
            return (CommandNode<T>) field.get(node);
        } catch (Throwable ignored) {
            UNWRAPPED_CACHED_SUPPORTED = false;
            return null;
        }
    }

    @NotNull
    private static Field clientNodeField() throws NoSuchFieldException {
        if (CLIENT_NODE_FIELD == null) {
            CLIENT_NODE_FIELD = CommandNode.class.getField("clientNode");
            CLIENT_NODE_FIELD.setAccessible(true);
        }

        return CLIENT_NODE_FIELD;
    }

    @NotNull
    private static Field unwrappedCachedField() throws NoSuchFieldException {
        if (UNWRAPPED_CACHED_FIELD == null) {
            UNWRAPPED_CACHED_FIELD = CommandNode.class.getField("unwrappedCached");
            UNWRAPPED_CACHED_FIELD.setAccessible(true);
        }

        return UNWRAPPED_CACHED_FIELD;
    }
}
