package org.nbfalcon.pythonCoverage.coveragePy;

import com.intellij.coverage.CoverageDataManager;
import com.intellij.coverage.CoverageSuitesBundle;
import com.intellij.psi.PsiFileSystemItem;

public interface AnnotatorWithMembership {
    boolean isCovered(PsiFileSystemItem fileOrDir, CoverageSuitesBundle currentSuite, CoverageDataManager dataManager);
}
