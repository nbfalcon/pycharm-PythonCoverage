package org.nbfalcon.pythonCoverage.coveragePy;

import com.intellij.coverage.CoverageAnnotator;
import com.intellij.coverage.CoverageSuitesBundle;
import com.intellij.coverage.view.*;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.DumbAwareToggleAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.*;
import com.intellij.util.ui.ColumnInfo;
import com.jetbrains.python.PythonFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nbfalcon.pythonCoverage.i18n.PythonCoverageBundle;
import org.nbfalcon.pythonCoverage.settings.PythonCoverageProjectSettings;

import java.awt.*;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CoveragePyViewExtension extends DirectoryCoverageViewExtension {
    private static final Predicate<AbstractTreeNode<?>> FILTER_PYTHON_FILES = (node) -> {
        final Object value = node.getValue();
        return value instanceof PsiFile
                ? coveragePySupports((PsiFile) value)
                : !Objects.equals(node.getName(), Project.DIRECTORY_STORE_FOLDER);
    };

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
                            if (FILTER_PYTHON_FILES.test(child)) {
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

    private void processPackage(AbstractTreeNode<?> node, List<AbstractTreeNode<?>> outResult) {
        outResult.add(node);
        for (AbstractTreeNode<?> child : super.getChildrenNodes(node)) {
            if (child.getValue() instanceof PsiDirectory) {
                processPackage(child, outResult);
            }
        }
    }

    @Override
    public @NotNull List<AbstractTreeNode<?>> createTopLevelNodes() {
        return getChildrenNodes(createRootNode());
    }

    @Override
    public @NotNull AbstractTreeNode<?> createRootNode() {
        final VirtualFile projectFile = VirtualFileManager.getInstance().findFileByUrl("file://" + myProject.getBasePath());
        return new CoveragePyRootNode(myProject,
                PsiManager.getInstance(myProject).findDirectory(projectFile),
                this.mySuitesBundle, myStateBean);
    }

    @Override
    public List<AbstractTreeNode<?>> getChildrenNodes(AbstractTreeNode node) {
        final Predicate<AbstractTreeNode<?>> filter = settings.coverageViewFilterIncluded
                ? this::hasCoverage
                : FILTER_PYTHON_FILES;
        final List<AbstractTreeNode<?>> children = super.getChildrenNodes(node);
        if (!myStateBean.myFlattenPackages) {
            return children.stream().filter(filter).collect(Collectors.toList());
        }
        else {
            final List<AbstractTreeNode<?>> nodes = new ArrayList<>();
            for (AbstractTreeNode<?> child : children) {
                if (filter.test(child)) {
                    if (child.getValue() instanceof PsiDirectory) processPackage(child, nodes);
                    else nodes.add(child);
                }
            }
            return nodes;
        }
    }
}
