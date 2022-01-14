package org.nbfalcon.pythonCoverage.coveragePy;

import com.intellij.coverage.BaseCoverageSuite;
import com.intellij.coverage.CoverageEngine;
import com.intellij.coverage.CoverageFileProvider;
import com.intellij.coverage.CoverageRunner;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class CoveragePySuite extends BaseCoverageSuite {
    private static final String HAS_RUN_KEY_S = "HAS_RUN";
    public boolean hasRun;

    public CoveragePySuite() {
        super();
    }

    public CoveragePySuite(String name, @Nullable CoverageFileProvider fileProvider, long lastCoverageTimeStamp,
                           boolean coverageByTestEnabled, boolean tracingEnabled, boolean trackTestFolders, CoverageRunner coverageRunner) {
        super(name, fileProvider, lastCoverageTimeStamp, coverageByTestEnabled, tracingEnabled, trackTestFolders, coverageRunner);
    }

    @Override
    public void readExternal(Element element) throws InvalidDataException {
        super.readExternal(element);
        hasRun = Boolean.parseBoolean(element.getAttributeValue(HAS_RUN_KEY_S));
    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {
        super.writeExternal(element);
        element.setAttribute(HAS_RUN_KEY_S, String.valueOf(hasRun));
    }

    @Override
    public @NotNull CoverageEngine getCoverageEngine() {
        return CoveragePyEngine.getInstance();
    }

    @Override
    public boolean canRemove() {
        return true;
    }

    @Override
    public void deleteCachedCoverageData() {
        // No point if the suite hasn't run already, as coverage.py will overwrite the data file itself
        if (hasRun && new File(getCoverageDataFileName()).isFile()) {
            super.deleteCachedCoverageData();
        }
    }
}