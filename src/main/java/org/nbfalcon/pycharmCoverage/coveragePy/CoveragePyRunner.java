package org.nbfalcon.pycharmCoverage.coveragePy;

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
import java.util.List;

public class CoveragePyRunner extends CoverageRunner {
    private static final Logger LOG = Logger.getInstance(CoveragePyRunner.class);

    private static int getNLines(List<Integer> lines) {
        return lines.isEmpty() ? 0 : lines.get(lines.size() - 1);
    }

    public static CoveragePyRunner getInstance() {
        return getInstance(CoveragePyRunner.class);
    }

    private static String joinPaths(String base, String... paths) {
        String[] filtered =
                Arrays.stream(paths).filter((path) -> !path.isEmpty() && !path.equals("/")).toArray(String[]::new);
        return Paths.get(base, filtered).toFile().getAbsolutePath();
    }

    // private static ProjectData parseCoverageOutput(CoveragePyLoaderJSON.CoverageOutput data) {
    //     ProjectData result = new ProjectData();
    //     for (Map.Entry<String, CoveragePyLoaderJSON.FileData> file : data.files.entrySet()) {
    //         final ClassData classData = result.getOrCreateClassData(file.getKey());
    //
    //         final CoveragePyLoaderJSON.FileData fileData = file.getValue();
    //         int nLines = Math.max(getNLines(fileData.executedLines), getNLines(fileData.missingLines));
    //         final LineData[] lines = new LineData[nLines];
    //         for (int executedLine : fileData.executedLines) {
    //             final LineData lineData = new LineData(executedLine, null);
    //             lineData.setHits(1);
    //             lines[executedLine - 1] = lineData;
    //         }
    //         for (int missingLine : fileData.missingLines) {
    //             final LineData lineData = new LineData(missingLine, null);
    //             lineData.setHits(0);
    //             lines[missingLine - 1] = lineData;
    //         }
    //
    //         classData.setLines(lines);
    //     }
    //     return result;

    private static String package2Path(String packageName) {
        return packageName.replace('.', '/');
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
                    // FIXME: i18n
                    String missing = (line.missingBranches == null) ? null
                            : (line.missingBranches.equals("exit") ? "<end of file>" : "line " + line.missingBranches);
                    String desc = "Missing branch: " + missing;
                    LineData lineData = new LineData(line.number, desc);
                    lineData.setHits(line.hits);
                    lineData.setStatus(!line.branch ? (line.hits == 0 ? LineCoverage.NONE : LineCoverage.FULL)
                            : (line.missingBranches == null ? LineCoverage.FULL : LineCoverage.PARTIAL));
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
        } catch (IOException e) {
            LOG.warn(e);
            return null;
        }
        catch (InterruptedException e) {
            return null;
        }
    }

    // @Override
    // public @Nullable ProjectData loadCoverageData(@NotNull File sessionDataFile, @Nullable CoverageSuite baseCoverageSuite) {
    //     try {
    //         Process process = new ProcessBuilder()
    //                 .command("python3.9", "-m", "coverage", "json", "-o", "-")
    //                 .directory(sessionDataFile)
    //                 .start();
    //         InputStream input = process.getInputStream();
    //         BufferedReader reader = new BufferedReader(new InputStreamReader(input));
    //         final CoveragePyLoaderJSON.CoverageOutput data = CoveragePyLoaderJSON.loadCoverageJsonSync(reader);
    //         if (data == null) {
    //             LOG.warn("coverage.py json command returned invalid json");
    //             return null;
    //         }
    //         return parseCoverageOutput(data);
    //     } catch (IOException e) {
    //         LOG.warn(e);
    //         return null;
    //     }
    // }

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
