package org.nbfalcon.pythonCoverage.coveragePy;

import com.intellij.coverage.SimpleCoverageAnnotator;
import com.intellij.openapi.project.Project;

public class CoveragePyAnnotator extends SimpleCoverageAnnotator {
    public CoveragePyAnnotator(Project project) {
        super(project);
    }

    public static CoveragePyAnnotator getInstance(Project project) {
        return project.getService(CoveragePyAnnotator.class);
    }
}
