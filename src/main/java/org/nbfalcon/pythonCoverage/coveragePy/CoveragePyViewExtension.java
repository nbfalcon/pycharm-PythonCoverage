package org.nbfalcon.pythonCoverage.coveragePy;

import com.intellij.coverage.CoverageAnnotator;
import com.intellij.coverage.CoverageSuitesBundle;
import com.intellij.coverage.view.*;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.DumbAwareToggleAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiManager;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.EdtInvocationManager;
import com.jetbrains.python.psi.PyUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nbfalcon.pythonCoverage.i18n.PythonCoverageBundle;
import org.nbfalcon.pythonCoverage.settings.PythonCoverageProjectSettings;
import org.nbfalcon.pythonCoverage.util.ideaUtil.CoverageAnnotatorUtil;
import org.nbfalcon.pythonCoverage.util.ideaUtil.CoverageViewUpdater;

import java.awt.*;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class CoveragePyViewExtension extends DirectoryCoverageViewExtension {
    private final PythonCoverageProjectSettings settings;

    public CoveragePyViewExtension(Project project, CoverageAnnotator annotator, CoverageSuitesBundle suitesBundle, CoverageViewManager.StateBean stateBean) {
        super(project, annotator, suitesBundle, stateBean);
        this.settings = PythonCoverageProjectSettings.getInstance(project);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public ColumnInfo[] createColumnInfos() {
        return new ColumnInfo[]{
                new ElementColumnInfo(),
                new PercentageCoverageColumnInfo(1, PythonCoverageBundle.message("viewExtension.column.line%"), mySuitesBundle, myStateBean)};
    }

    private boolean hasCoverage(AbstractTreeNode<?> node) {
        return hasCoverage((PsiFileSystemItem) node.getValue());
    }

    private boolean hasCoverage(PsiFileSystemItem item) {
        return CoverageAnnotatorUtil.hasCoverage(item, myAnnotator, mySuitesBundle, myCoverageDataManager);
    }

    @Override
    public boolean supportFlattenPackages() {
        return true;
    }

    @Override
    public @Nullable
    PsiElement getElementToSelect(Object object) {
        if (object instanceof PsiElement) {
            return (PsiElement) object;
        } else if (object instanceof VirtualFile) {
            VirtualFile fileOrDir = (VirtualFile) object;
            final PsiManager psiManager = PsiManager.getInstance(myProject);
            return fileOrDir.isDirectory() ? psiManager.findDirectory(fileOrDir) : psiManager.findFile(fileOrDir);
        }
        return null;
    }

    private static String getDirComponentName(PsiDirectory dir) {
        return dir.getName() + (PyUtil.isExplicitPackage(dir) ? "." : "/");
    }

    private static List<PsiDirectory> getPsiDirectoryPath(PsiDirectory root, PsiDirectory until) {
        List<PsiDirectory> path = new ArrayList<>();
        while (until != null && until != root) {
            path.add(until);
            until = until.getParentDirectory();
        }
        Collections.reverse(path);
        return path;
    }

    private static @NotNull
    String getFQNamePrefixToRoot(PsiDirectory root, PsiDirectory until) {
        StringBuilder result = new StringBuilder();
        for (PsiDirectory dir : getPsiDirectoryPath(root, until)) {
            result.append(getDirComponentName(dir));
        }
        return result.toString();
    }

    /**
     * @apiNote Always run in a PSI read action!
     */
    private void processPackageSubdirectories(@NotNull Predicate<PsiFileSystemItem> filter,
                                              @NotNull PsiDirectory curDir,
                                              @NotNull List<AbstractTreeNode<?>> outResult,
                                              @NotNull String fqNamePrefix) {
        for (PsiDirectory subdir : curDir.getSubdirectories()) {
            if (filter.test(subdir)) {
                outResult.add(new CoveragePyListNode(myProject, subdir, mySuitesBundle, myStateBean,
                        fqNamePrefix + subdir.getName()));
                processPackageSubdirectories(filter, subdir, outResult,
                        fqNamePrefix + getDirComponentName(subdir));
            }
        }
    }

    @Override
    public @NotNull
    List<AnAction> createExtraToolbarActions() {
        return List.of(new DumbAwareToggleAction(
                PythonCoverageBundle.messageLazy("viewExtension.filterIncludedInCoverage"),
                PythonCoverageBundle.messageLazy("viewExtension.filterIncludedInCoverageDescription"),
                // FIXME: scale Icon to 13x13
                AllIcons.RunConfigurations.TrackCoverage) {
            @Override
            public boolean isSelected(@NotNull AnActionEvent anActionEvent) {
                return settings.coverageViewFilterIncluded;
            }

            @Override
            public void setSelected(@NotNull AnActionEvent anActionEvent, boolean b) {
                final InputEvent ev = anActionEvent.getInputEvent();
                if (ev != null) {
                    final CoverageView view = getCoverageViewFromEvent(ev);
                    if (view != null) {
                        CoverageViewUpdater.updateView(view, b, settings,
                                myProject, myAnnotator, mySuitesBundle, myStateBean, myCoverageDataManager);
                    }
                } else {
                    settings.coverageViewFilterIncluded = b;
                }
            }

            private CoverageView getCoverageViewFromEvent(InputEvent event) {
                Object source = event.getSource();
                while (source != null && !(source instanceof CoverageView)) {
                    source = ((Component) source).getParent();
                }
                return (CoverageView) source;
            }
        });
    }

    @Override
    public @NotNull
    AbstractTreeNode<?> createRootNode() {
        return new CoveragePyRootNode(myProject, getProjectPsiDirectory(), this.mySuitesBundle, myStateBean);
    }

    @Override
    public @NotNull
    List<AbstractTreeNode<?>> createTopLevelNodes() {
        return createTopLevelNodes(getProjectPsiDirectory(), "");
    }

    private List<AbstractTreeNode<?>> createTopLevelNodes(PsiDirectory projectDir,
                                                          @NotNull String fqNamePrefix) {
        List<AbstractTreeNode<?>> children = new ArrayList<>();

        final Predicate<PsiFileSystemItem> filter = getFilterAndMaybeUpdate();
        ReadAction.run(() -> processPackageSubdirectories(filter, projectDir, children, fqNamePrefix));
        filterPsiFilesToNodes(filter, projectDir.getFiles(), children);

        return children;
    }

    @Override
    public List<AbstractTreeNode<?>> getChildrenNodes(AbstractTreeNode node) {
        final Object value = node.getValue();
        if (!(value instanceof PsiDirectory)) return Collections.emptyList();
        final PsiDirectory nodeDir = (PsiDirectory) value;

        final Predicate<PsiFileSystemItem> filter = getFilterAndMaybeUpdate();

        final List<AbstractTreeNode<?>> children;
        if (!myStateBean.myFlattenPackages) {
            children = new ArrayList<>();
            ReadAction.run(() -> {
                filterPsiFilesToNodes(filter, nodeDir.getSubdirectories(), children);
                filterPsiFilesToNodes(filter, nodeDir.getFiles(), children);
            });
        } else {
            // intellij-java doesn't cache this either
            final PsiDirectory projectDir = getProjectPsiDirectory();
            final String prefix = getFQNamePrefixToRoot(projectDir, nodeDir);
            children = createTopLevelNodes(nodeDir, prefix);
        }
        for (AbstractTreeNode<?> child : children) {
            child.setParent(node);
        }
        return children;
    }

    private Predicate<PsiFileSystemItem> getFilterAndMaybeUpdate() {
        if (settings.coverageViewFilterIncluded) {
            if (myAnnotator instanceof CoveragePyAnnotator) {
                CoveragePyAnnotator annotator = (CoveragePyAnnotator) myAnnotator;
                if (annotator.isUpdating()) {
                    EdtInvocationManager.getInstance().invokeLater(() -> {
                        // view can be null while constructing the Coverage tool window, so get it on the EDT later
                        CoverageView view = CoverageViewManager.getInstance(myProject).getToolwindow(mySuitesBundle);
                        if (view != null) {
                            annotator.maybeUpdateLater(view,
                                    () -> CoverageViewUpdater.updateView(view, false, null,
                                            myProject, myAnnotator, mySuitesBundle, myStateBean, myCoverageDataManager));
                        }
                    });
                    // Show at least some files while we're updating
                    return CoveragePyAnnotator::isAcceptedPythonFile;
                }
            }
            return this::hasCoverage;
        }
        return CoveragePyAnnotator::isAcceptedPythonFile;
    }

    private void filterPsiFilesToNodes(@NotNull Predicate<PsiFileSystemItem> filter,
                                       @NotNull PsiFileSystemItem[] nodes,
                                       @NotNull List<AbstractTreeNode<?>> outChildren) {
        for (PsiFileSystemItem node : nodes) {
            if (filter.test(node)) {
                outChildren.add(new CoverageListNode(myProject, node, mySuitesBundle, myStateBean));
            }
        }
    }

    @NotNull
    private PsiDirectory getProjectPsiDirectory() {
        final VirtualFile projectFile = VirtualFileManager.getInstance().findFileByUrl("file://" + myProject.getBasePath());
        if (projectFile == null) throw new NullPointerException("Project disappeared as a VirtualFile!");
        final PsiDirectory result = PsiManager.getInstance(myProject).findDirectory(projectFile);
        if (result == null) throw new NullPointerException("Project does not exist in PSI");
        return result;
    }
}
