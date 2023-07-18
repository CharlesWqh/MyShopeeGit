package com.shopee.shopeegit;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.util.function.Supplier;

public final class MyGitBundle extends DynamicBundle {
    public static final @NonNls String BUNDLE = "properties.GitBundle";
    private static final MyGitBundle INSTANCE = new MyGitBundle();

    private MyGitBundle() { super(BUNDLE); }

    public static @NotNull @Nls String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object @NotNull ... params) {
        return INSTANCE.getMessage(key, params);
    }

    public static @NotNull Supplier<@Nls String> messagePointer(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object @NotNull ... params) {
        return INSTANCE.getLazyMessage(key, params);
    }

    /**
     * @deprecated prefer {@link #message(String, Object...)} instead
     */
    @Deprecated
    public static @NotNull @Nls String getString(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key) {
        return message(key);
    }
}
