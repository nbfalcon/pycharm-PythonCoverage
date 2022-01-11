package org.nbfalcon.pycharmCoverage.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@State(name = "PycharmCoverage.Settings", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public class PycharmCoverageProjectSettings implements PersistentStateComponent<PycharmCoverageProjectSettings> {
    public String coveragePyModule = null;
    public WhichRunner coveragePyWhichRunner;

    public static PycharmCoverageProjectSettings getInstance(Project project) {
        return project.getService(PycharmCoverageProjectSettings.class);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (getClass() != o.getClass()) return false;

        PycharmCoverageProjectSettings that = (PycharmCoverageProjectSettings) o;

        if (!Objects.equals(coveragePyModule, that.coveragePyModule)) return false;
        return coveragePyWhichRunner != that.coveragePyWhichRunner;
    }

    @Override
    public PycharmCoverageProjectSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull PycharmCoverageProjectSettings state) {
        this.coveragePyModule = state.coveragePyModule;
        this.coveragePyWhichRunner = state.coveragePyWhichRunner;
    }

    public @NotNull String getCoveragePyModule() {
        return (coveragePyModule == null) ? "-m coverage" : coveragePyModule;
    }

    public enum WhichRunner {
        BUILT_IN, CUSTOM
    }
}
