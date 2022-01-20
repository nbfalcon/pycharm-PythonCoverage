package org.nbfalcon.pythonCoverage.coveragePy;

import com.intellij.coverage.CoverageAnnotator;
import com.intellij.coverage.CoverageSuitesBundle;
import com.intellij.coverage.view.*;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.DumbAwareToggleAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.*;
import com.intellij.util.ui.ColumnInfo;
import com.jetbrains.python.PythonFileType;
import com.jetbrains.python.psi.PyUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nbfalcon.pythonCoverage.i18n.PythonCoverageBundle;
import org.nbfalcon.pythonCoverage.settings.PythonCoverageProjectSettings;

import java.awt.*;
import java.awt.event.InputEvent;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;

public class CoveragePyViewExtension extends DirectoryCoverageViewExtension {
    public static boolean isAcceptedPythonFile(PsiFileSystemItem value) {
        return value instanceof PsiFile
                ? coveragePySupports((PsiFile) value)
                : !Objects.equals(value.getName(), Project.DIRECTORY_STORE_FOLDER);
    }

    PythonCoverageProjectSettings settings;

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

    private static boolean coveragePySupports(PsiFile file) {
        final FileType fileType = file.getFileType();
        // TODO: Is "Jinja2" actually true for PyCharm Professional?
        return fileType == PythonFileType.INSTANCE || fileType.getName().equals("Jinja2");
    }

    private boolean hasCoverage(AbstractTreeNode<?> node) {
        return getPercentage(1, node) != null;
    }

    private boolean hasCoverage(PsiFileSystemItem item) {
        String percentage = (item instanceof PsiDirectory)
                ? myAnnotator.getDirCoverageInformationString((PsiDirectory) item, mySuitesBundle, myCoverageDataManager)
                : myAnnotator.getFileCoverageInformationString((PsiFile) item, mySuitesBundle, myCoverageDataManager);
        return percentage != null;
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
                        updateView(view, b);
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

            // HACK: this sets settings.coverageViewFilterIncluded and then causes the view to be updated. The latter
            // involves numerous horrible hacks seem more like an exploit.
            private void updateView(CoverageView view, boolean filterIncluded) {
                if (!filterIncluded) {
                    settings.coverageViewFilterIncluded = false;
                    final AbstractTreeNode<?> selected = (AbstractTreeNode<?>) view.getData(CommonDataKeys.NAVIGATABLE.getName());
                    if (selected != null) {
                        view.select(((PsiFileSystemItem) selected.getValue()).getVirtualFile());
                    } else {
                        final String projectPath = myProject.getBasePath();
                        if (projectPath == null) return;
                        final VirtualFile projectDir = VirtualFileManager.getInstance().findFileByUrl("file://" + projectPath);
                        if (projectDir == null) return;
                        final PsiDirectory projectPsi = PsiManager.getInstance(myProject).findDirectory(projectDir);
                        if (projectPsi == null) return;
                        PsiFileSystemItem found = null;
                        for (AbstractTreeNode<?> child : new CoverageListNode(myProject, projectPsi, mySuitesBundle, myStateBean).getChildren()) {
                            PsiFileSystemItem value = (PsiFileSystemItem) child.getValue();
                            if (isAcceptedPythonFile(value)) {
                                found = (PsiFileSystemItem) child.getValue();
                                break;
                            }
                        }
                        if (found != null) {
                            view.select(found.getVirtualFile());
                        }
                    }
                } else {
                    AbstractTreeNode<?> selected = (AbstractTreeNode<?>) view.getData(CommonDataKeys.NAVIGATABLE.getName());
                    final AbstractTreeNode<?> next = findNextNodeWithCoverage(selected);
                    settings.coverageViewFilterIncluded = true;
                    if (next != null) {
                        if (next.getParent() != null) {
                            view.select(((PsiFileSystemItem) next.getValue()).getVirtualFile());
                        } else {
                            final CoverageListNode fakeParent = new CoverageListNode(myProject, (PsiNamedElement) next.getValue(), mySuitesBundle, myStateBean);
                            fakeParent.setParent(next);
                            next.setParent(fakeParent);
                            view.goUp();
                            next.setParent(null);
                            view.goUp();
                        }
                    }
                }
            }

            private AbstractTreeNode<?> findNextNodeWithCoverage(AbstractTreeNode<?> selected) {
                while (selected != null && selected.getParent() != null && !hasCoverage(selected)) {
                    final AbstractTreeNode<?> sibling = findNextSiblingWithCoverage(selected);
                    if (sibling != null) return sibling;
                    selected = selected.getParent();
                }
                return selected;
            }

            @SuppressWarnings("unchecked")
            private AbstractTreeNode<?> findNextSiblingWithCoverage(AbstractTreeNode<?> node) {
                final AbstractTreeNode<?> parent = node.getParent();
                if (parent == null) return null;

                final Collection<? extends AbstractTreeNode<?>> children1 = parent.getChildren();
                if (!(children1 instanceof List)) return null;

                final List<? extends AbstractTreeNode<?>> children = (List<? extends AbstractTreeNode<?>>) children1;
                final int index = children.indexOf(node);
                if (index == -1) return null;
                for (int i = index; i < children.size(); i++) {
                    if (hasCoverage(children.get(i))) {
                        return children.get(i);
                    }
                }
                for (int i = index; i >= 0; i--) {
                    if (hasCoverage(children.get(i))) {
                        return children.get(i);
                    }
                }
                return null;
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

        final Predicate<PsiFileSystemItem> filter = getFilter();
        ReadAction.run(() -> processPackageSubdirectories(filter, projectDir, children, fqNamePrefix));
        filterPsiFilesToNodes(filter, projectDir.getFiles(), children);

        return children;
    }

    @Override
    public List<AbstractTreeNode<?>> getChildrenNodes(AbstractTreeNode node) {
        final Object value = node.getValue();
        if (!(value instanceof PsiDirectory)) return Collections.emptyList();
        final PsiDirectory nodeDir = (PsiDirectory) value;

        final Predicate<PsiFileSystemItem> filter = getFilter();

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

    private Predicate<PsiFileSystemItem> getFilter() {
        return settings.coverageViewFilterIncluded
                ? this::hasCoverage
                : CoveragePyViewExtension::isAcceptedPythonFile;
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
