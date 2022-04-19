package org.nbfalcon.pythonCoverage.bundle;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public class PythonCoverageDependencies {
    private PythonCoverageDependencies() {
    }

    public static @NotNull Path getCoveragePyModulePath() {
        IdeaPluginDescriptor thisPlugin =
                PluginManagerCore.getPlugin(PluginId.getId("org.nbfalcon.python-coverage"));
        assert thisPlugin != null;
        return thisPlugin.getPluginPath().resolve("dependencies/coveragepy/coverage/__main__.py");
    }
}
