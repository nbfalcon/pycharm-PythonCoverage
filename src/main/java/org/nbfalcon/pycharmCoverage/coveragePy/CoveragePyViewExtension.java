package org.nbfalcon.pycharmCoverage.coveragePy;

import com.intellij.coverage.CoverageAnnotator;
import com.intellij.coverage.CoverageSuitesBundle;
import com.intellij.coverage.view.CoverageViewManager;
import com.intellij.coverage.view.DirectoryCoverageViewExtension;
import com.intellij.coverage.view.ElementColumnInfo;
import com.intellij.coverage.view.PercentageCoverageColumnInfo;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.ui.ColumnInfo;
import com.jetbrains.python.PythonFileType;
import org.jetbrains.annotations.NotNull;
import org.nbfalcon.pycharmCoverage.i18n.PycharmCoverageBundle;
import org.nbfalcon.pycharmCoverage.settings.PycharmCoverageProjectSettings;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class CoveragePyViewExtension extends DirectoryCoverageViewExtension {
    @Override
    public @NotNull List<AnAction> createExtraToolbarActions() {
        return List.of(new ToggleAction("Branch coverage") {
            @Override
            public boolean isSelected(@NotNull AnActionEvent anActionEvent) {
                return PycharmCoverageProjectSettings.getInstance(myProject).enableBranchCoverage;
            }

            @Override
            public void setSelected(@NotNull AnActionEvent anActionEvent, boolean b) {
                PycharmCoverageProjectSettings.getInstance(myProject).enableBranchCoverage = b;
            }
        }, new ToggleAction("coverage.py: Use Module") {
            @Override
            public boolean isSelected(@NotNull AnActionEvent anActionEvent) {
                return PycharmCoverageProjectSettings.getInstance(myProject).coveragePyUseModule;
            }

            @Override
            public void setSelected(@NotNull AnActionEvent anActionEvent, boolean b) {
                PycharmCoverageProjectSettings.getInstance(myProject).coveragePyUseModule = b;
            }
        });
    }

    public CoveragePyViewExtension(Project project, CoverageAnnotator annotator, CoverageSuitesBundle suitesBundle, CoverageViewManager.StateBean stateBean) {
        super(project, annotator, suitesBundle, stateBean);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public ColumnInfo[] createColumnInfos() {
        return new ColumnInfo[]{
                new ElementColumnInfo(),
                new PercentageCoverageColumnInfo(1, PycharmCoverageBundle.message("viewExtension.column.line%"), mySuitesBundle, myStateBean)};
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
