package org.nbfalcon.pythonCoverage.i18n;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.util.function.Supplier;

public class PythonCoverageBundle extends DynamicBundle {
    public static final PythonCoverageBundle INSTANCE = new PythonCoverageBundle();
    @NonNls
    public static final String BUNDLE = "PythonCoverage";

    private PythonCoverageBundle() {
        super(BUNDLE);
    }

    @NotNull
    public static @Nls
    String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, @NotNull Object... params) {
        return INSTANCE.getMessage(key, params);
    }

    @NotNull
    public static @Nls
    Supplier<String> messageLazy(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key,
                                 @NotNull Object... params) {
        return INSTANCE.getLazyMessage(key, params);
    }
}
