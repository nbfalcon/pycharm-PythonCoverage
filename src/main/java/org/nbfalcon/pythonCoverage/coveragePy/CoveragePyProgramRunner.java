package org.nbfalcon.pythonCoverage.coveragePy;

import com.intellij.coverage.CoverageExecutor;
import com.intellij.coverage.CoverageHelper;
import com.intellij.coverage.CoverageRunnerData;
import com.intellij.coverage.CoverageSuite;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.configurations.*;
import com.intellij.execution.configurations.coverage.CoverageEnabledConfiguration;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.runners.RunContentBuilder;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.application.AppUIExecutor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.jetbrains.python.debugger.PyDebugRunner;
import com.jetbrains.python.run.AbstractPythonRunConfiguration;
import com.jetbrains.python.run.CommandLinePatcher;
import com.jetbrains.python.run.PythonCommandLineState;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nbfalcon.pythonCoverage.settings.PythonCoverageProjectSettings;

import java.io.File;

public class CoveragePyProgramRunner implements ProgramRunner<RunnerSettings> {
    private static RunProfile unwrapProfile(@NotNull RunProfile profile) {
        return (profile instanceof WrappingRunConfiguration)
                ? ((WrappingRunConfiguration<?>) profile).getPeer()
                : profile;
    }

    @Override
    public @NotNull @NonNls String getRunnerId() {
        return "Coverage.py";
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return executorId.equals(CoverageExecutor.EXECUTOR_ID)
                && unwrapProfile(profile) instanceof AbstractPythonRunConfiguration;
    }

    @Override
    public RunnerSettings createConfigurationData(@NotNull ConfigurationInfoProvider settingsProvider) {
        return new CoverageRunnerData();
    }

    @Override
    public void execute(@NotNull ExecutionEnvironment environment) throws ExecutionException {
        final RunProfileState state = environment.getState();
        if (state == null) return;
        final RunnerSettings runnerSettings = environment.getRunnerSettings();
        final RunProfile profile = environment.getRunProfile();
        if (!(profile instanceof AbstractPythonRunConfiguration)) return;
        final AbstractPythonRunConfiguration<?> conf = (AbstractPythonRunConfiguration<?>) profile;
        final CoverageEnabledConfiguration covConf = CoverageEnabledConfiguration.getOrCreate(conf);

        // FIXME: don't use submit
        final Project project = environment.getProject();
        ExecutionManager.getInstance(project).startRunProfile(environment, () -> AppUIExecutor.onUiThread().submit(() -> {
            // PyDebugRunner also does this for some reason
            FileDocumentManager.getInstance().saveAllDocuments();
            final PythonCommandLineState statePy = (PythonCommandLineState) state;
            final ExecutionResult result;
            try {
                result = statePy.execute(environment.getExecutor(),
                        createPatchers(environment, statePy, covConf.getCoverageFilePath(),
                                PythonCoverageProjectSettings.getInstance(project)));
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
            final CoverageSuite suite = covConf.getCurrentCoverageSuite();
            // if (covConf instanceof CoveragePyEnabledConfiguration)
            //     ((CoveragePyEnabledConfiguration) covConf).coverageDirectory = conf.getWorkingDirectorySafe();
            if (suite instanceof CoveragePySuite) ((CoveragePySuite) suite).hasRun = true;

            final RunContentDescriptor descriptor = new RunContentBuilder(result, environment)
                    .showRunContent(environment.getContentToReuse());
            CoverageHelper.attachToProcess(conf, result.getProcessHandler(), runnerSettings);
            // result.getProcessHandler().startNotify();
            return descriptor;
        }));
    }

    private CommandLinePatcher[] createPatchers(@NotNull ExecutionEnvironment environment,
                                                PythonCommandLineState statePy,
                                                @Nullable @NonNls String outputPath,
                                                PythonCoverageProjectSettings settings) {
        return new CommandLinePatcher[]{
                PyDebugRunner.createRunConfigPatcher(statePy, environment.getRunProfile()),
                createCoveragePyPatcher(outputPath, settings)};
    }

    private CommandLinePatcher createCoveragePyPatcher(@Nullable @NonNls String outputPath, PythonCoverageProjectSettings settings) {
        return generalCommandLine -> {
            final ParamsGroup coverageGroup = generalCommandLine.getParametersList().getParamsGroup(PythonCommandLineState.GROUP_COVERAGE);
            assert coverageGroup != null;
            if (settings.coveragePyUseModule) coverageGroup.addParameters(settings.getCoveragePyModuleArgs());
            else coverageGroup.addParameters("-m", "coverage"); // FIXME: use bundled
            coverageGroup.addParameters("run");
            // The JavaCoverageEngine just doesn't download the file if the path is null, and lets the CoverageRunner
            // proceed as if nothing happened, so we just do the same.
            if (outputPath != null) coverageGroup.addParameters("-o", new File(outputPath).getAbsolutePath());
            if (settings.enableBranchCoverage) coverageGroup.addParameter("--branch");
            coverageGroup.addParameter("--");
        };
    }
}
