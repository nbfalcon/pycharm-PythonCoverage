package org.nbfalcon.pythonCoverage.coveragePy;

import com.intellij.coverage.CoverageDataManager;
import com.intellij.coverage.CoverageSuitesBundle;
import com.intellij.psi.PsiFileSystemItem;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface AnnotatorWithDetail {
    @Nullable
    @Nls
    String getDetailedCoverageInformationString(@NotNull PsiFileSystemItem fileOrDir,
                                                @NotNull CoverageSuitesBundle currentSuite,
                                                @NotNull CoverageDataManager manager);

    @Nullable
    @Nls
    String getBranchCoverageInformationString(@NotNull PsiFileSystemItem fileOrDir,
                                              @NotNull CoverageSuitesBundle currentSuite,
                                              @NotNull CoverageDataManager manager);
}
