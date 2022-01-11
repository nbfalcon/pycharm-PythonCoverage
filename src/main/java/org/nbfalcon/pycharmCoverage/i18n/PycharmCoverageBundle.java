package org.nbfalcon.pycharmCoverage.i18n;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

public class PycharmCoverageBundle extends DynamicBundle {
    public static final PycharmCoverageBundle INSTANCE = new PycharmCoverageBundle();
    @NonNls
    public static final String BUNDLE = "PycharmCoverage";

    private PycharmCoverageBundle() {
        super(BUNDLE);
    }

    @NotNull
    public static @Nls String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object @NotNull ... params) {
        return INSTANCE.getMessage(key, params);
    }
}
