package org.nbfalcon.pythonCoverage.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.nbfalcon.pythonCoverage.util.ShellArgumentTokenizer;

import java.util.List;

@State(name = "PythonCoverage.Settings", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public class PythonCoverageProjectSettings implements PersistentStateComponent<PythonCoverageProjectSettings> {
    private static final List<String> M_COVERAGE = List.of("-m", "coverage");

    public String coveragePyModule = null;
    public boolean coveragePyUseModule = false;

    public boolean enableBranchCoverage = false;

    /**
     * Make the Coverage tool window only display files included in coverage.
     */
    public boolean coverageViewFilterIncluded = false;

    @Override
    public void loadState(@NotNull PythonCoverageProjectSettings state) {
        this.coveragePyModule = state.coveragePyModule;
        this.coveragePyUseModule = state.coveragePyUseModule;
        this.enableBranchCoverage = state.enableBranchCoverage;
        this.coverageViewFilterIncluded = state.coverageViewFilterIncluded;
    }

    public List<String> getCoveragePyModuleArgs() {
        return coveragePyModule != null
                ? ShellArgumentTokenizer.tokenize(coveragePyModule)
                : M_COVERAGE;
    }

    public static PythonCoverageProjectSettings getInstance(Project project) {
        return project.getService(PythonCoverageProjectSettings.class);
    }

    @Override
    public PythonCoverageProjectSettings getState() {
        return this;
    }
}