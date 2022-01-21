package org.nbfalcon.pythonCoverage.coveragePy;

import com.fasterxml.jackson.core.JsonParseException;
import com.intellij.coverage.CoverageEngine;
import com.intellij.coverage.CoverageOptionsConfigurable;
import com.intellij.coverage.CoverageRunner;
import com.intellij.coverage.CoverageSuite;
import com.intellij.icons.AllIcons;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.LineCoverage;
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.ProjectData;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nbfalcon.pythonCoverage.i18n.PythonCoverageBundle;
import org.nbfalcon.pythonCoverage.settings.PythonCoverageApplicationSettings;
import org.nbfalcon.pythonCoverage.settings.SettingsUtil;
import org.nbfalcon.pythonCoverage.util.ideaUtil.InterruptableModalTask;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;

public class CoveragePyRunner extends CoverageRunner {
    private static final Logger LOG = Logger.getInstance(CoveragePyRunner.class);

    public static CoveragePyRunner getInstance() {
        return getInstance(CoveragePyRunner.class);
    }

    private static String joinPaths(String base, String... paths) {
        String[] filtered =
                Arrays.stream(paths).filter((path) -> !path.isEmpty() && !path.equals("/")).toArray(String[]::new);
        return Paths.get(base, filtered).toFile().getAbsolutePath();
    }

    private static String package2Path(String packageName) {
        return packageName.replace('.', '/');
    }

    private static Integer parseIntSafe(String s) {
        try {
            return s == null ? null : Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Nullable
    private static ProjectData loadCoverageDataSync(@NotNull File sessionDataFile,
                                                    @Nullable Project projectForWarnings) {
        try {
            final ProcessBuilder builder = SettingsUtil.createProcess(
                    PythonCoverageApplicationSettings.getInstance().getCoveragePyLoaderPythonCommand(),
                    "-m", "coverage", "xml",
                    "-c", sessionDataFile.getAbsolutePath(), "-o", "-");
            final Process process = builder.start();
            // DEBUG: Thread.sleep(10000);
            // FIXME: remove, using a custom inputstream
            process.waitFor(); // Allow interrupt() via cancel
            InputStream input = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            final CoveragePyLoaderXML.CoverageOutput data = CoveragePyLoaderXML.loadFromXML(reader);
            if (data == null) {
                LOG.warn("coverage.py xml returned invalid output");
                return null;
            }
            return loadCoverageOutput(data);
        } catch (IOException e) {
            // Handle "No data to report." (printed to stdout for some reason)
            if (e instanceof JsonParseException && e.getMessage().startsWith("Unexpected character 'N'")) {
                return null;
            } else if (e.getMessage().startsWith("Cannot run program")) {
                showNotFoundLoaderExecutableBalloon(projectForWarnings);
            }
            LOG.warn(e);
        } catch (InterruptedException ignored) {
        }
        // NOTE: null means nothing new to contribute, but the coverage window may still pop up, contains the old
        // coverage data. This is not a bug in the plugin, maybe in IDEA (but probably not).
        return null;
    }

    private static void showNotFoundLoaderExecutableBalloon(@Nullable Project projectForNotification) {
        new Notification("Python Coverage",
                PythonCoverageBundle.message("notifications.loader.loaderExecutableNotFound"), NotificationType.ERROR)
                .setDisplayId("pythonCoverage.coveragePyRunner.loaderExecutableNotFound")
                .setIcon(AllIcons.RunConfigurations.TrackCoverage)
                .addAction(NotificationAction.create(PythonCoverageBundle.message("notifications.loader.executableNotFoundConfigure"),
                        (event, notification) -> {
                            final Project project = event.getProject();
                            if (project != null) showCoverageSettings(project);
                        }))
                .notify(projectForNotification);
    }

    private static void showCoverageSettings(@NotNull Project project) {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, CoverageOptionsConfigurable.class);
    }

    @Override
    @Nullable
    public ProjectData loadCoverageData(@NotNull File sessionDataFile, @Nullable CoverageSuite baseCoverageSuite) {
        final @Nullable Project project = baseCoverageSuite == null ? null : baseCoverageSuite.getProject();
        if (ApplicationManager.getApplication().isDispatchThread()) {
            return InterruptableModalTask.runSyncForResult(
                    project,
                    PythonCoverageBundle.message("loader.progressTitle"),
                    () -> loadCoverageDataSync(sessionDataFile, project));
        }
        return loadCoverageDataSync(sessionDataFile, project);
    }

    private static ProjectData loadCoverageOutput(CoveragePyLoaderXML.CoverageOutput data) {
        String rootDir = data.sources.get(0);
        ProjectData result = new ProjectData();
        for (CoveragePyLoaderXML.PackageData covPackage : data.packages) {
            for (CoveragePyLoaderXML.ClassCoverage covClass : covPackage.classes) {
                String path = joinPaths(rootDir, package2Path(covPackage.name), covClass.name);
                ClassData classData = result.getOrCreateClassData(path);

                int nLines = (covClass.lines.isEmpty()) ? 0 : covClass.lines.get(covClass.lines.size() - 1).number;
                LineData[] lines = new LineData[nLines];
                for (CoveragePyLoaderXML.Line line : covClass.lines) {
                    LineData lineData = new LineData(line.number, null);
                    lineData.setHits(line.hits);
                    String missingBranches = line.missingBranches;
                    lineData.setStatus(!line.branch ? (line.hits == 0 ? LineCoverage.NONE : LineCoverage.FULL)
                            : (missingBranches == null ? LineCoverage.FULL : LineCoverage.PARTIAL));
                    int[] keys = null;
                    if (Objects.equals(missingBranches, "exit")) keys = new int[]{-1};
                    else {
                        final Integer l = parseIntSafe(missingBranches);
                        if (l != null) keys = new int[]{l};
                    }
                    if (keys != null) lineData.addSwitch(0, keys);
                    lineData.fillArrays();
                    lines[line.number - 1] = lineData;
                }
                classData.setLines(lines);
            }
        }
        return result;
    }

    @Override
    public @NotNull
    @NonNls
    String getPresentableName() {
        return "Coverage.py";
    }

    @Override
    public @NotNull
    @NonNls
    String getId() {
        return "CoveragePyRunner";
    }

    @Override
    public @NotNull @NonNls String getDataFileExtension() {
        return "coverage";
    }

    @Override
    public boolean acceptsCoverageEngine(@NotNull CoverageEngine engine) {
        return engine instanceof CoveragePyEngine;
    }
}
