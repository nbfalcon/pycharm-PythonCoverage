package org.nbfalcon.pythonCoverage.coveragePy;

import com.intellij.coverage.AbstractCoverageProjectViewNodeDecorator;
import com.intellij.coverage.BaseCoverageAnnotator;
import com.intellij.coverage.CoverageDataManager;
import com.intellij.coverage.CoverageSuitesBundle;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.RootsProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packageDependencies.ui.PackageDependenciesNode;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.tree.project.ProjectFileNode;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class CoveragePyProjectNodeAnnotator extends AbstractCoverageProjectViewNodeDecorator {
    public CoveragePyProjectNodeAnnotator(@NotNull Project project) {
        super(project);
    }

    @Override
    public void decorate(ProjectViewNode<?> projectViewNode, PresentationData presentationData) {
        Project project = projectViewNode.getProject();
        if (project == null) return;

        final CoveragePyAnnotator annotator = CoveragePyAnnotator.getInstance(project);
        CoverageDataManager data = getCoverageDataManager(project);
        if (data == null) return;
        final CoverageSuitesBundle suites = data.getCurrentSuitesBundle();
        if (suites == null || !(suites.getCoverageEngine() instanceof CoveragePyEngine)) return;

        final Object value = projectViewNode.getValue();
        if (isPackageElement(value)) {
            final PsiManager psiManager = PsiManager.getInstance(project);

            BaseCoverageAnnotator.DirCoverageInfo info = new BaseCoverageAnnotator.DirCoverageInfo();

            for (VirtualFile root : ((RootsProvider) value).getRoots()) {
                PsiDirectory psi = psiManager.findDirectory(root);
                if (psi != null) {
                    BaseCoverageAnnotator.DirCoverageInfo oneInfo = annotator.getDirCoverageInfo(psi, suites);
                    if (oneInfo != null) {
                        info.totalLineCount += oneInfo.totalLineCount;
                        info.coveredLineCount += oneInfo.coveredLineCount;
                        info.totalFilesCount += oneInfo.totalFilesCount;
                        info.coveredFilesCount += oneInfo.coveredFilesCount;
                    }
                }
            }

            doAnnotate(info, annotator, presentationData);
        } else if (value instanceof PsiFile && CoveragePyAnnotator.isAcceptedPythonFile((PsiFile) value)
                && projectViewNode.getParent() != null
                && isPackageElement(projectViewNode.getParent().getValue())) {
            final PsiFile psi = (PsiFile) value;

            final String info = annotator.getFileCoverageInformationString(psi, suites, data);
            if (info != null) {
                presentationData.setLocationString(info);
            }
        } else if (value instanceof ProjectFileNode) {
            final VirtualFile thisFile = ((ProjectFileNode) value).getVirtualFile();
            // Normal files in the project view are "PsiFile"s, and they are handled by built-in IntelliJ machinery.
            // That is, we don't need to do anything special; packages are "ScopeTreeViewModel$FileNode"s though...
            if (thisFile.isDirectory()) {
                final PsiDirectory psi = PsiManager.getInstance(project).findDirectory(thisFile);
                if (psi != null) {
                    BaseCoverageAnnotator.DirCoverageInfo info = annotator.getDirCoverageInfo(psi, suites);
                    if (info != null) {
                        doAnnotate(info, annotator, presentationData);
                    }
                }
            }
        }
    }

    private static void doAnnotate(BaseCoverageAnnotator.DirCoverageInfo info, CoveragePyAnnotator annotator, PresentationData presentationData) {
        final String files = annotator.getFilesCoverageInformationString(info);
        final String lines = annotator.getLinesCoverageInformationString(info);
        if (files != null || lines != null) {
            final String decoration;
            if (files != null && lines != null) {
                decoration = files + ", " + lines;
            } else decoration = Objects.requireNonNullElse(files, lines);
            presentationData.setLocationString(decoration);
        }
    }

    private static boolean isPackageElement(Object value) {
        return value.getClass().getName().equals("com.intellij.ide.projectView.impl.nodes.PackageElement")
                && value instanceof RootsProvider;
    }

    @Override
    public void decorate(PackageDependenciesNode packageDependenciesNode, ColoredTreeCellRenderer coloredTreeCellRenderer) {
    }

}
