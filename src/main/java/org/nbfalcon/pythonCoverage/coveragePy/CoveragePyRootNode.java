package org.nbfalcon.pythonCoverage.coveragePy;

import com.intellij.coverage.CoverageSuitesBundle;
import com.intellij.coverage.view.CoverageListRootNode;
import com.intellij.coverage.view.CoverageViewManager;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiNamedElement;
import org.jetbrains.annotations.NotNull;
import org.nbfalcon.pythonCoverage.settings.PythonCoverageProjectSettings;

import java.util.Collection;
import java.util.List;

/**
 * Like [CoverageListRootNode], but with support for dropping the top-level-packages cache.
 */
public class CoveragePyRootNode extends CoverageListRootNode {
    private List<AbstractTreeNode<?>> myTopLevelPackagesFilterIncluded;

    public CoveragePyRootNode(Project project, @NotNull PsiNamedElement classOrPackage, CoverageSuitesBundle bundle, CoverageViewManager.StateBean stateBean) {
        super(project, classOrPackage, bundle, stateBean);
    }

    private Collection<AbstractTreeNode<?>> getTopLevelPackagesFiltered() {
        if (myTopLevelPackagesFilterIncluded== null) {
            myTopLevelPackagesFilterIncluded = myBundle.getCoverageEngine()
                    .createCoverageViewExtension(myProject, myBundle, myStateBean)
                    .createTopLevelNodes();
            for (AbstractTreeNode<?> child : myTopLevelPackagesFilterIncluded) {
                child.setParent(this);
            }
        }
        return myTopLevelPackagesFilterIncluded;
    }

    @Override
    public @NotNull
    Collection<? extends AbstractTreeNode<?>> getChildren() {
        if (myStateBean.myFlattenPackages
                && PythonCoverageProjectSettings.getInstance(myProject).coverageViewFilterIncluded) {
            return getTopLevelPackagesFiltered();
        }
        return super.getChildren();
    }
}
