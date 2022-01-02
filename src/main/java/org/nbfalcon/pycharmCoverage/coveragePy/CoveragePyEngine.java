package org.nbfalcon.pycharmCoverage.coveragePy;

import com.intellij.coverage.*;
import com.intellij.coverage.view.CoverageViewExtension;
import com.intellij.coverage.view.CoverageViewManager;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.coverage.CoverageEnabledConfiguration;
import com.intellij.execution.testframework.AbstractTestProxy;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsActions;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.python.PythonFileType;
import com.jetbrains.python.psi.PyFile;
import com.jetbrains.python.run.AbstractPythonRunConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class CoveragePyEngine extends CoverageEngine {
    public static CoveragePyEngine getInstance() {
        return EP_NAME.findExtensionOrFail(CoveragePyEngine.class);
    }

    @Override
    public boolean isApplicableTo(@NotNull RunConfigurationBase<?> conf) {
        return conf instanceof AbstractPythonRunConfiguration;
    }

    @Override
    public boolean canHavePerTestCoverage(@NotNull RunConfigurationBase<?> conf) {
        return false;
    }

    @Override
    public @NotNull CoverageEnabledConfiguration createCoverageEnabledConfiguration(@NotNull RunConfigurationBase<?> conf) {
        return new CoveragePyEnabledConfiguration(conf);
    }

    @Override
    public @Nullable CoverageSuite createCoverageSuite(@NotNull CoverageRunner covRunner, @NotNull String name, @NotNull CoverageFileProvider coverageDataFileProvider, String @Nullable [] filters, long lastCoverageTimeStamp, @Nullable String suiteToMerge, boolean coverageByTestEnabled, boolean tracingEnabled, boolean trackTestFolders) {
        // FIXME: handle filters, also in CoveragePyEnabledConfiguration
        // FIXME: inject the path as python arg instead
        return new CoveragePySuite(name, null, System.currentTimeMillis(), false, false, false, covRunner);
    }

    @Override
    public @Nullable CoverageSuite createCoverageSuite(@NotNull CoverageRunner covRunner, @NotNull String name, @NotNull CoverageFileProvider coverageDataFileProvider, String @Nullable [] filters, long lastCoverageTimeStamp, @Nullable String suiteToMerge, boolean coverageByTestEnabled, boolean tracingEnabled, boolean trackTestFolders, Project project) {
        return createCoverageSuite(covRunner, name, coverageDataFileProvider, filters, lastCoverageTimeStamp, suiteToMerge, coverageByTestEnabled, tracingEnabled, trackTestFolders);
    }

    @Override
    public @Nullable CoverageSuite createCoverageSuite(@NotNull CoverageRunner covRunner, @NotNull String name,
                                                       @NotNull CoverageFileProvider coverageDataFileProvider,
                                                       @NotNull CoverageEnabledConfiguration config) {
        if (!(config.getConfiguration() instanceof AbstractPythonRunConfiguration)) return null;
        if (!(config instanceof CoveragePyEnabledConfiguration)) return null;
        final CoveragePyEnabledConfiguration configPy = (CoveragePyEnabledConfiguration) config;

        assert configPy.coverageDirectory != null;
        CoverageFileProvider provider = new DefaultCoverageFileProvider(
                Paths.get(configPy.coverageDirectory, ".coverage").toFile());
        return new CoveragePySuite(name,
                provider, System.currentTimeMillis(), false, false, false, covRunner);
    }

    @Override
    public @Nullable CoverageSuite createEmptyCoverageSuite(@NotNull CoverageRunner coverageRunner) {
        return new CoveragePySuite();
    }

    @Override
    public @NotNull CoverageAnnotator getCoverageAnnotator(Project project) {
        return CoveragePyAnnotator.getInstance(project);
    }

    @Override
    public boolean coverageEditorHighlightingApplicableTo(@NotNull PsiFile psiFile) {
        return psiFile instanceof PyFile;
    }

    @Override
    public boolean acceptedByFilters(@NotNull PsiFile psiFile, @NotNull CoverageSuitesBundle suite) {
        return psiFile instanceof PyFile;
    }

    @Override
    public boolean recompileProjectAndRerunAction(@NotNull Module module, @NotNull CoverageSuitesBundle suite, @NotNull Runnable chooseSuiteAction) {
        return false;
    }

    @Override
    public @Nullable String getQualifiedName(@NotNull File outputFile, @NotNull PsiFile sourceFile) {
        VirtualFile vf = sourceFile.getVirtualFile();
        return (vf == null) ? outputFile.getAbsolutePath() : vf.getPath();
    }

    @Override
    public @NotNull Set<String> getQualifiedNames(@NotNull PsiFile sourceFile) {
        VirtualFile file = sourceFile.getVirtualFile();
        return (file != null) ? Set.of(file.getPath()) : Collections.emptySet();
    }

    @Override
    public List<PsiElement> findTestsByNames(String @NotNull [] testNames, @NotNull Project project) {
        return Collections.emptyList();
    }

    @Override
    public @Nullable String getTestMethodName(@NotNull PsiElement element, @NotNull AbstractTestProxy testProxy) {
        return null;
    }

    @Override
    public @NlsActions.ActionText String getPresentableText() {
        return "Coverage.Py";
    }

    @Override
    public boolean coverageProjectViewStatisticsApplicableTo(VirtualFile fileOrDir) {
        return !fileOrDir.isDirectory() && fileOrDir.getFileType() == PythonFileType.INSTANCE;
    }

    @Override
    public CoverageViewExtension createCoverageViewExtension(Project project, CoverageSuitesBundle suiteBundle,
                                                             CoverageViewManager.StateBean stateBean) {
        return new CoveragePyViewExtension(project, getCoverageAnnotator(project), suiteBundle, stateBean);
    }
}