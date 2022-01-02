package org.nbfalcon.pycharmCoverage.coveragePy;

import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.coverage.CoverageEnabledConfiguration;
import org.jetbrains.annotations.NotNull;

public class CoveragePyEnabledConfiguration extends CoverageEnabledConfiguration {
    public String coverageDirectory = null;

    public CoveragePyEnabledConfiguration(@NotNull RunConfigurationBase<?> configuration) {
        super(configuration);
        setCoverageRunner(CoveragePyRunner.getInstance());
    }
}
