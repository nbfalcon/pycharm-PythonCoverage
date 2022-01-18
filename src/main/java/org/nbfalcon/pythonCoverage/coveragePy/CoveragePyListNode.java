package org.nbfalcon.pythonCoverage.coveragePy;

import com.intellij.coverage.CoverageSuitesBundle;
import com.intellij.coverage.view.CoverageListNode;
import com.intellij.coverage.view.CoverageViewManager;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import org.jetbrains.annotations.NotNull;

public class CoveragePyListNode extends CoverageListNode {
    private final String fqName;

    public CoveragePyListNode(Project project, @NotNull PsiNamedElement classOrPackage, CoverageSuitesBundle bundle,
                              CoverageViewManager.StateBean stateBean,
                              String fqName) {
        super(project, classOrPackage, bundle, stateBean);
        this.fqName = fqName;
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
        if (myStateBean.myFlattenPackages) {
            presentation.setPresentableText(fqName);
            final PsiElement value = (PsiElement) getValue();
            ApplicationManager.getApplication().runReadAction(() -> presentation.setIcon(value.getIcon(0)));
        } else super.update(presentation);
    }
}
