package org.nbfalcon.pycharmCoverage.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.nbfalcon.pycharmCoverage.util.ShellArgumentTokenizer;

import java.util.List;

@State(name = "PycharmCoverage.Settings", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public class PycharmCoverageProjectSettings implements PersistentStateComponent<PycharmCoverageProjectSettings> {
    private static final List<String> M_COVERAGE = List.of("-m", "coverage");

    public String coveragePyModule = null;
    public boolean coveragePyUseModule = false;

    public boolean enableBranchCoverage = false;

    /**
     * Make the Coverage tool window only display files included in coverage.
     */
    public boolean coverageViewFilterIncluded = false;

    public List<String> getCoveragePyModuleArgs() {
        return coveragePyModule != null
                ? ShellArgumentTokenizer.tokenize(coveragePyModule)
                : M_COVERAGE;
    }

    public static PycharmCoverageProjectSettings getInstance(Project project) {
        return project.getService(PycharmCoverageProjectSettings.class);
    }

    @Override
    public PycharmCoverageProjectSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull PycharmCoverageProjectSettings state) {
        this.coveragePyModule = state.coveragePyModule;
        this.coveragePyUseModule = state.coveragePyUseModule;
    }
}