package me.vaperion.blade.tokenizer.input;

@SuppressWarnings("unused")
public enum InputOption {
    /**
     * Assume quoted parameters are allowed, ignoring the command's settings.
     */
    ASSUME_QUOTED,

    /**
     * Disallow parsing any flags from the input.
     */
    DISALLOW_FLAGS,

    /**
     * Only parse flags that are at the end of the input.
     * <p>
     * Treat everything else as normal parameters.
     */
    FLAGS_AT_END,
}
