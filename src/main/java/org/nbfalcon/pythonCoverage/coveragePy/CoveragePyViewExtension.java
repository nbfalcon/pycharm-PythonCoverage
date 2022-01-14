package org.nbfalcon.pythonCoverage.coveragePy;

import com.intellij.coverage.CoverageAnnotator;
import com.intellij.coverage.CoverageSuitesBundle;
import com.intellij.coverage.view.*;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiManager;
import com.intellij.util.ui.ColumnInfo;
import com.jetbrains.python.PythonFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nbfalcon.pythonCoverage.i18n.PythonCoverageBundle;
import org.nbfalcon.pythonCoverage.settings.PythonCoverageProjectSettings;

import java.awt.*;
import java.awt.event.InputEvent;
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
        return List.of(new ToggleAction(
                PythonCoverageBundle.messageLazy("viewExtension.filterIncludedInCoverage")) {
            @Override
            public boolean isSelected(@NotNull AnActionEvent anActionEvent) {
                return settings.coverageViewFilterIncluded;
            }

            @Override
            public void setSelected(@NotNull AnActionEvent anActionEvent, boolean b) {
                final InputEvent ev = anActionEvent.getInputEvent();
                if (ev != null) {
                    final CoverageView view = getCoverageViewFromEv(ev.getSource());
                    if (view != null) {
                        final AbstractTreeNode<?> nodeToSelect = getNodeToSelectAfterUpdate(view);
                        settings.coverageViewFilterIncluded = b;
                        updateTree(view, nodeToSelect);
                    }
                } else {
                    settings.coverageViewFilterIncluded = b;
                }
            }

            private CoverageView getCoverageViewFromEv(Object source) {
                while (source != null && !(source instanceof CoverageView)) {
                    source = ((Component) source).getParent();
                }
                return (CoverageView) source;
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

            // HACK: this is horrible: we basically select the currently selected element to update the tree, which
            // can break any time
            // FIXME: does not work if there is no covered file and the option is turned off again
            private void updateTree(CoverageView view, @Nullable AbstractTreeNode<?> nodeToSelect) {
                if (nodeToSelect != null) {
                    final Object selectedPsi = nodeToSelect.getValue();
                    if (selectedPsi instanceof PsiFileSystemItem) {
                        final VirtualFile file = ((PsiFileSystemItem) selectedPsi).getVirtualFile();
                        if (file != null) {
                            view.select(file);
                        }
                    }
                }
            }

            @Nullable
            private AbstractTreeNode<?> getNodeToSelectAfterUpdate(CoverageView view) {
                AbstractTreeNode<?> selected = (AbstractTreeNode<?>) view.getData(CommonDataKeys.NAVIGATABLE.getName());
                if (selected != null) {
                    if (!hasCoverage(selected)) {
                        // Otherwise, the parent gets selected
                        do {
                            final AbstractTreeNode<?> next = findNextSiblingWithCoverage(selected);
                            if (next != null) {
                                selected = next;
                                break;
                            } else {
                                final AbstractTreeNode<?> parent = selected.getParent();
                                selected = parent;
                                if (parent instanceof CoverageListRootNode) {
                                    // selected = (AbstractTreeNode<?>) ((List) parent.getChildren()).get(0);
                                    break;
                                }
                            }
                        } while (!hasCoverage(selected));
                    }
                }
                return selected;
            }
        });
    }

    @Override
    public List<AbstractTreeNode<?>> getChildrenNodes(AbstractTreeNode node) {
        final Predicate<AbstractTreeNode<?>> filter = settings.coverageViewFilterIncluded
                ? this::hasCoverage
                : FILTER_PYTHON_FILES;
        return super.getChildrenNodes(node).stream().filter(filter).collect(Collectors.toList());
    }
}
