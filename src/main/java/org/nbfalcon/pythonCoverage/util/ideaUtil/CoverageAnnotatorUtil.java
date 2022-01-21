package org.nbfalcon.pythonCoverage.util.ideaUtil;

import com.intellij.coverage.CoverageAnnotator;
import com.intellij.coverage.CoverageDataManager;
import com.intellij.coverage.CoverageSuitesBundle;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;

public class CoverageAnnotatorUtil {
    /**
     * @param myAnnotator Should return null for file/dir coverage if not covered.
     */
    public static boolean hasCoverage(PsiFileSystemItem item,
                                      CoverageAnnotator myAnnotator,
                                      CoverageSuitesBundle mySuitesBundle,
                                      CoverageDataManager myCoverageDataManager) {
        String percentage = (item instanceof PsiDirectory)
                ? myAnnotator.getDirCoverageInformationString((PsiDirectory) item, mySuitesBundle, myCoverageDataManager)
                : myAnnotator.getFileCoverageInformationString((PsiFile) item, mySuitesBundle, myCoverageDataManager);
        return percentage != null;
    }
}
