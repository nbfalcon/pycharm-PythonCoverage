package org.nbfalcon.pycharmCoverage.coveragePy;

import com.intellij.coverage.CoverageAnnotator;
import com.intellij.coverage.CoverageSuitesBundle;
import com.intellij.coverage.view.CoverageViewManager;
import com.intellij.coverage.view.DirectoryCoverageViewExtension;
import com.intellij.coverage.view.ElementColumnInfo;
import com.intellij.coverage.view.PercentageCoverageColumnInfo;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.ui.ColumnInfo;
import com.jetbrains.python.PythonFileType;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class CoveragePyViewExtension extends DirectoryCoverageViewExtension {
    public CoveragePyViewExtension(Project project, CoverageAnnotator annotator, CoverageSuitesBundle suitesBundle, CoverageViewManager.StateBean stateBean) {
        super(project, annotator, suitesBundle, stateBean);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public ColumnInfo[] createColumnInfos() {
        return new ColumnInfo[]{
                new ElementColumnInfo(),
                new PercentageCoverageColumnInfo(1, "Lines, %", mySuitesBundle, myStateBean)};
    }

    @Override
    public List<AbstractTreeNode<?>> getChildrenNodes(AbstractTreeNode node) {
        return super.getChildrenNodes(node).stream().filter((child) -> {
            final Object value = child.getValue();
            return value instanceof PsiFile
                    ? ((PsiFile) value).getFileType() == PythonFileType.INSTANCE
                    : !Objects.equals(child.getName(), Project.DIRECTORY_STORE_FOLDER);
        }).collect(Collectors.toList());
    }
}
