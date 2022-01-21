package org.nbfalcon.pythonCoverage.util.ideaUtil;

import com.intellij.coverage.CoverageAnnotator;
import com.intellij.coverage.CoverageDataManager;
import com.intellij.coverage.CoverageSuitesBundle;
import com.intellij.coverage.view.CoverageListNode;
import com.intellij.coverage.view.CoverageView;
import com.intellij.coverage.view.CoverageViewManager;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.nbfalcon.pythonCoverage.coveragePy.CoveragePyAnnotator;
import org.nbfalcon.pythonCoverage.settings.PythonCoverageProjectSettings;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

/**
 * Implements a way to update a {@link CoverageView}.
 *
 * This entire class is a massive hack, so the API and pass-trough variables are horrible.
 */
public class CoverageViewUpdater {
    // HACK: this sets settings.coverageViewFilterIncluded and then causes the view to be updated. The
    // implementation involves numerous horrible hacks and seems more like an exploit.
    public static void updateView(
            @NotNull CoverageView view,
            boolean filterIncluded, @Nullable PythonCoverageProjectSettings settings,
            Project myProject,
            CoverageAnnotator myAnnotator, CoverageSuitesBundle mySuitesBundle,
            CoverageViewManager.StateBean myStateBean, CoverageDataManager myCoverageDataManager) {
        if (!filterIncluded) {
            if (settings != null) settings.coverageViewFilterIncluded = false;
            final AbstractTreeNode<?> selected = (AbstractTreeNode<?>) view.getData(CommonDataKeys.NAVIGATABLE.getName());
            if (selected != null) {
                refreshViaNodeHack(view, selected);
                view.select(((PsiFileSystemItem) selected.getValue()).getVirtualFile());
            } else {
                final String projectPath = myProject.getBasePath();
                if (projectPath == null) return;
                final VirtualFile projectDir = VirtualFileManager.getInstance().findFileByUrl("file://" + projectPath);
                if (projectDir == null) return;
                final PsiDirectory projectPsi = PsiManager.getInstance(myProject).findDirectory(projectDir);
                if (projectPsi == null) return;
                ReadAction.run(() -> {
                    for (PsiElement file : projectPsi.getChildren()) {
                        if (file instanceof PsiFileSystemItem && CoveragePyAnnotator.isAcceptedPythonFile((PsiFileSystemItem) file)) {
                            view.select(((PsiFileSystemItem) file).getVirtualFile());
                            break;
                        }
                    }
                });
            }
        } else {
            AbstractTreeNode<?> selected = (AbstractTreeNode<?>) view.getData(CommonDataKeys.NAVIGATABLE.getName());
            final AbstractTreeNode<?> next = findNextNodeWithCoverage(selected, myAnnotator, mySuitesBundle,
                    myCoverageDataManager);
            if (settings != null) settings.coverageViewFilterIncluded = true;
            if (next != null) {
                if ((AbstractTreeNode<?>) next.getParent() != null) {
                    refreshViaNodeHack(view, selected);
                    view.select(((PsiFileSystemItem) next.getValue()).getVirtualFile());
                } else {
                    final CoverageListNode fakeParent = new CoverageListNode(
                            myProject, (PsiNamedElement) next.getValue(),
                            mySuitesBundle, myStateBean);
                    fakeParent.setParent(next);
                    next.setParent(fakeParent);
                    view.goUp();
                    next.setParent(null);
                    view.goUp();
                }
            }
        }
    }

    /**
     * Cause VIEW to be refreshed by abusing its SELECTED element.
     */
    private static void refreshViaNodeHack(CoverageView view, AbstractTreeNode<?> selected) {
        final AbstractTreeNode<?> oldParent = selected.getParent();
        selected.setParent(selected);
        view.goUp();
        selected.setParent(oldParent);
    }

    private static AbstractTreeNode<?> findNextNodeWithCoverage(AbstractTreeNode<?> selected,
                                                                CoverageAnnotator myAnnotator,
                                                                CoverageSuitesBundle mySuitesBundle,
                                                                CoverageDataManager myCoverageDataManager) {
        while (selected != null && selected.getParent() != null
                && !CoverageAnnotatorUtil.hasCoverage((PsiFileSystemItem) selected.getValue(),
                myAnnotator, mySuitesBundle, myCoverageDataManager)) {
            final AbstractTreeNode<?> sibling = findNextSiblingWithCoverage(selected, myAnnotator, mySuitesBundle,
                    myCoverageDataManager);
            if (sibling != null) return sibling;
            selected = selected.getParent();
        }
        return selected;
    }

    @SuppressWarnings("unchecked")
    private static AbstractTreeNode<?> findNextSiblingWithCoverage(
            AbstractTreeNode<?> node,
            CoverageAnnotator myAnnotator,
            CoverageSuitesBundle mySuitesBundle,
            CoverageDataManager myCoverageDataManager) {
        final AbstractTreeNode<?> parent = node.getParent();
        if (parent == null) return null;

        final Collection<? extends AbstractTreeNode<?>> children1 = parent.getChildren();
        if (!(children1 instanceof List)) return null;

        // FIXME: select next sibling by PSI so that flatten works elegantly (parent is selected)
        final List<? extends AbstractTreeNode<?>> children = (List<? extends AbstractTreeNode<?>>) children1;
        final int index = children.indexOf(node);
        if (index == -1) return null;
        for (int i = index; i < children.size(); i++) {
            if (CoverageAnnotatorUtil.hasCoverage(
                    (PsiFileSystemItem) children.get(i).getValue(), myAnnotator, mySuitesBundle, myCoverageDataManager)) {
                return children.get(i);
            }
        }
        for (int i = index; i >= 0; i--) {
            if (CoverageAnnotatorUtil.hasCoverage(
                    (PsiFileSystemItem) children.get(i).getValue(), myAnnotator, mySuitesBundle, myCoverageDataManager)) {
                return children.get(i);
            }
        }
        return null;
    }
}
