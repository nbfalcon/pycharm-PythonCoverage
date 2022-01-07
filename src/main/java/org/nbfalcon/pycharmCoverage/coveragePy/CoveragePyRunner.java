package org.nbfalcon.pycharmCoverage.coveragePy;

import com.fasterxml.jackson.core.JsonParseException;
import com.intellij.coverage.CoverageEngine;
import com.intellij.coverage.CoverageRunner;
import com.intellij.coverage.CoverageSuite;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.LineCoverage;
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.ProjectData;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
                    if (missingBranches != null) {
                        if (missingBranches.equals("exit")) {
                            keys = new int[]{-1};
                        }
                        else {
                            boolean haveExit = false;
                            if (missingBranches.endsWith(",exit")) {
                                missingBranches = missingBranches.substring(0, missingBranches.length() - 5);
                                haveExit = true;
                            }
                            final Integer l = parseIntSafe(missingBranches);
                            keys = l == null ? (haveExit ? new int[]{-1} : null)
                                    : haveExit ? new int[]{l, -1}
                                    : new int[]{l};
                        }
                    }
                    if (keys != null) {
                        lineData.addSwitch(0, keys);
                    }
                    lineData.fillArrays();
                    lines[line.number - 1] = lineData;
                }
                classData.setLines(lines);
            }
        }
        return result;
    }

    @Override
    public @Nullable ProjectData loadCoverageData(@NotNull File sessionDataFile, @Nullable CoverageSuite baseCoverageSuite) {
        try {
            Process process = new ProcessBuilder()
                    // FIXME: choose a python installation. Also, what if not installed?
                    .command("python3.9", "-m", "coverage", "xml",
                            "-c", sessionDataFile.getAbsolutePath(),
                            "-o", "-")
                    .start();
            process.waitFor();
            InputStream input = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            final CoveragePyLoaderXML.CoverageOutput data = CoveragePyLoaderXML.loadFromXML(reader);
            if (data == null) {
                LOG.warn("coverage.py xml returned invalid output");
                return null;
            }
            return loadCoverageOutput(data);
        }
        catch (IOException e) {
            // Handle "No data to report." (printed to stdout for some reason)
            if (e instanceof JsonParseException && e.getMessage().startsWith("Unexpected character 'N'")) return null;
            LOG.warn(e);
            return null;
        }
        catch (InterruptedException e) {
            return null;
        }
    }

    @Override
    public @NotNull @NonNls String getPresentableName() {
        return "coverage.py";
    }

    @Override
    public @NotNull @NonNls String getId() {
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
