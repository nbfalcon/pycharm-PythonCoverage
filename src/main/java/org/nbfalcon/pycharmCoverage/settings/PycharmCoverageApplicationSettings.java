package org.nbfalcon.pycharmCoverage.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;

@State(name = "PycharmCoverage.Global.Settings", storages = @Storage(value = "pycharmCoverage.xml", roamingType = RoamingType.PER_OS))
public class PycharmCoverageApplicationSettings implements PersistentStateComponent<PycharmCoverageApplicationSettings> {
    public String coveragePyLoaderPythonCommand = "python";

    public static PycharmCoverageApplicationSettings getInstance() {
        return ApplicationManager.getApplication().getService(PycharmCoverageApplicationSettings.class);
    }

    @Override
    public PycharmCoverageApplicationSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull PycharmCoverageApplicationSettings state) {
        this.coveragePyLoaderPythonCommand = state.coveragePyLoaderPythonCommand;
    }
}
