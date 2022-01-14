package org.nbfalcon.pythonCoverage.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;

@State(name = "PythonCoverage.Global.Settings",
        storages = @Storage(value = "pythonCoverage.xml", roamingType = RoamingType.PER_OS))
public class PythonCoverageApplicationSettings implements PersistentStateComponent<PythonCoverageApplicationSettings> {
    public String coveragePyLoaderPythonCommand = null;

    public String getCoveragePyLoaderPythonCommand() {
        return coveragePyLoaderPythonCommand == null ? "python" : coveragePyLoaderPythonCommand;
    }

    public static PythonCoverageApplicationSettings getInstance() {
        return ApplicationManager.getApplication().getService(PythonCoverageApplicationSettings.class);
    }

    @Override
    public PythonCoverageApplicationSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull PythonCoverageApplicationSettings state) {
        this.coveragePyLoaderPythonCommand = state.coveragePyLoaderPythonCommand;
    }
}
