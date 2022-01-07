package org.nbfalcon.pycharmCoverage.coveragePy;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

public class CoveragePyBundle extends DynamicBundle {
    public static final CoveragePyBundle INSTANCE = new CoveragePyBundle();
    @NonNls
    public static final String BUNDLE = "CoveragePy";

    private CoveragePyBundle() {
        super(BUNDLE);
    }

    @NotNull
    public static @Nls String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object @NotNull ... params) {
        return INSTANCE.getMessage(key, params);
    }
}
