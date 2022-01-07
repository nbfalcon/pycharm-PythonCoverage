package org.nbfalcon.pycharmCoverage.coveragePy;

import com.intellij.codeEditor.printing.ExportToHTMLSettings;
import com.intellij.coverage.*;
import com.intellij.coverage.view.CoverageViewExtension;
import com.intellij.coverage.view.CoverageViewManager;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.coverage.CoverageEnabledConfiguration;
import com.intellij.execution.testframework.AbstractTestProxy;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsActions;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.data.SwitchData;
import com.jetbrains.python.PythonFileType;
import com.jetbrains.python.psi.PyFile;
import com.jetbrains.python.run.AbstractPythonRunConfiguration;
import jetbrains.coverage.report.ReportBuilderFactory;
import jetbrains.coverage.report.SourceCodeProvider;
import jetbrains.coverage.report.html.HTMLReportBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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
        return new CoveragePySuite(name,
                coverageDataFileProvider, System.currentTimeMillis(), false, false, false, covRunner);
    }

    @Override
    public @Nullable CoverageSuite createEmptyCoverageSuite(@NotNull CoverageRunner coverageRunner) {
        return new CoveragePySuite();
    }

    @Override
    public @NotNull CoverageAnnotator getCoverageAnnotator(Project project) {
        return CoveragePyAnnotator.getInstance(project);
    }

    private static final SourceCodeProvider VF_FILE_CODE_PROVIDER = (file) -> {
        final VirtualFile vFile = VirtualFileManager.getInstance().findFileByUrl("file://" + file);
        if (vFile == null) return null;

        final byte[] contents = vFile.contentsToByteArray(true);
        return new String(contents, vFile.getCharset());
    };

    @Override
    public boolean coverageEditorHighlightingApplicableTo(@NotNull PsiFile psiFile) {
        return psiFile instanceof PyFile;
    }

    @Override
    public boolean isReportGenerationAvailable(@NotNull Project project, @NotNull DataContext dataContext, @NotNull CoverageSuitesBundle currentSuite) {
        return true;
    }

    @Override
    public String generateBriefReport(@NotNull Editor editor, @NotNull PsiFile psiFile, int lineNumber, int startOffset, int endOffset, @Nullable LineData lineData) {
        // FIXME: i18n
        if (lineData != null) {
            String hit = lineData.getHits() != 0
                    ? CoveragePyBundle.message("brief.lineHit")
                    : CoveragePyBundle.message("brief.lineNotHit");
            String missing = "";
            if (lineData.switchesCount() > 0) {
                final SwitchData switch0 = lineData.getSwitchData(0);
                final int[] keys = switch0.getKeys();
                if (keys != null && keys.length > 0) {
                    final int key = keys[0];
                    missing = "\n" + (key == -1
                            ? CoveragePyBundle.message("brief.missingJumpExit")
                            : CoveragePyBundle.message("brief.missingJumpLine", key));
                }
            }
            return hit + missing;
        } else {
            return CoveragePyBundle.message("brief.lineNotHit");
        }
    }

    @Override
    public void generateReport(@NotNull Project project, @NotNull DataContext dataContext, @NotNull CoverageSuitesBundle currentSuite) {
        long startNS = System.nanoTime();
        final ProjectData data = currentSuite.getCoverageData();
        if (data == null) return;
        final ExportToHTMLSettings settings = ExportToHTMLSettings.getInstance(project);

        long generationStartNS = System.nanoTime();
        final HTMLReportBuilder reportBuilder = ReportBuilderFactory.createHTMLReportBuilder();
        reportBuilder.setReportDir(new File(settings.OUTPUT_DIRECTORY));
        reportBuilder.generateReport(new CoveragePyGenerationData(data, VF_FILE_CODE_PROVIDER));

        long endNS = System.nanoTime();
        CoverageLogger.logHTMLReport(project,
                TimeUnit.NANOSECONDS.toMillis(endNS - startNS),
                TimeUnit.NANOSECONDS.toMillis(generationStartNS - startNS));
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