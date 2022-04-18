package org.nbfalcon.pythonCoverage.coveragePy;

import com.intellij.coverage.*;
import com.intellij.coverage.view.CoverageView;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.TestSourcesFilter;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.LineCoverage;
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.util.ui.EdtInvocationManager;
import com.jetbrains.python.PythonFileType;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.PropertyKey;
import org.nbfalcon.pythonCoverage.i18n.PythonCoverageBundle;
import org.nbfalcon.pythonCoverage.util.ideaUtil.CoverageViewUpdaterHack;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class CoveragePyAnnotator extends SimpleCoverageAnnotator implements AnnotatorWithMembership, AnnotatorWithDetail {
    public static class FileCoverageInfoEx extends BaseCoverageAnnotator.FileCoverageInfo {
        int fullyBranchCoveredLineCount;
    }

    public static class DirCoverageInfoEx extends BaseCoverageAnnotator.DirCoverageInfo {
        int fullyBranchCoveredFilesCount;
        int fullyBranchCoveredLineCount;
    }

    /**
     * Mapping of CoverageView => a hook that updates it once no longer updating.
     * The hooks are Runnable closures that invoke
     * {@link CoverageViewUpdaterHack#updateView}.
     *
     * @see "isUpdating"
     */
    private final Map<CoverageView, Runnable> onUpdateViewsHook = new HashMap<>();
    /**
     * Are the coverage infos currently being updated (createRenewRequest() is still running asynchronously)?
     */
    private boolean isUpdating = false;

    private int renewVersion = 0;

    public CoveragePyAnnotator(Project project) {
        super(project);
    }

    public static CoveragePyAnnotator getInstance(Project project) {
        return project.getService(CoveragePyAnnotator.class);
    }

    public static boolean isAcceptedPythonFile(PsiFileSystemItem value) {
        return value instanceof PsiFile ? coveragePySupports((PsiFile) value) : !Objects.equals(value.getName(), Project.DIRECTORY_STORE_FOLDER);
    }

    private static boolean coveragePySupports(PsiFile file) {
        final FileType fileType = file.getFileType();
        // TODO: we don't support Jinja
        // fileType.getName().equals("Jinja2");
        return fileType == PythonFileType.INSTANCE;
    }

    // Expose protected method
    @Override
    public @Nullable DirCoverageInfo getDirCoverageInfo(@NotNull PsiDirectory directory, @NotNull CoverageSuitesBundle currentSuite) {
        return super.getDirCoverageInfo(directory, currentSuite);
    }

    @Override
    protected @Nullable Runnable createRenewRequest(@NotNull CoverageSuitesBundle suite, @NotNull CoverageDataManager dataManager) {
        final Runnable renewRequest = super.createRenewRequest(suite, dataManager);
        if (renewRequest == null) return null;

        isUpdating = true;
        return () -> {
            renewRequest.run();
            EdtInvocationManager.getInstance().invokeLater(this::updateCoverageViews);
        };
    }

    @Override
    protected @Nullable DirCoverageInfoEx collectFolderCoverage(@NotNull VirtualFile dir, @NotNull CoverageDataManager dataManager, Annotator annotator, ProjectData projectInfo, boolean trackTestFolders, @NotNull ProjectFileIndex index, @NotNull CoverageEngine coverageEngine, Set<? super VirtualFile> visitedDirs, @NotNull Map<String, String> normalizedFiles2Files) {
        if (!visitedDirs.add(dir)) return null;

        final Boolean indexOk = ReadAction.compute(() -> index.isInContent(dir) && (shouldCollectCoverageInsideLibraryDirs() || !index.isInLibrary(dir)));
        if (!indexOk) return null;

        final boolean isTest = TestSourcesFilter.isTestSources(dir, getProject());
        if (isTest && !trackTestFolders) return null;

        final VirtualFile[] children = dataManager.doInReadActionIfProjectOpen(dir::getChildren);
        if (children != null) {
            DirCoverageInfoEx result = new DirCoverageInfoEx();
            for (VirtualFile child : children) {
                if (child.isDirectory()) {
                    final DirCoverageInfoEx childInfo = collectFolderCoverage(child, dataManager, annotator, projectInfo, trackTestFolders, index, coverageEngine, visitedDirs, normalizedFiles2Files);
                    if (childInfo != null) {
                        result.totalFilesCount += childInfo.totalFilesCount;
                        result.coveredFilesCount += childInfo.coveredFilesCount;
                        result.totalLineCount += childInfo.totalLineCount;
                        result.coveredLineCount += childInfo.coveredLineCount;

                        result.fullyBranchCoveredLineCount += childInfo.fullyBranchCoveredLineCount;
                        result.fullyBranchCoveredFilesCount += childInfo.fullyBranchCoveredFilesCount;
                    }
                } else if (coverageEngine.coverageProjectViewStatisticsApplicableTo(child)) {
                    final FileCoverageInfoEx childInfo = (FileCoverageInfoEx) collectBaseFileCoverage(child, annotator, projectInfo, normalizedFiles2Files);
                    if (childInfo != null) {
                        result.totalLineCount += childInfo.totalLineCount;
                        result.coveredLineCount += childInfo.coveredLineCount;

                        result.coveredFilesCount++;

                        result.fullyBranchCoveredLineCount += childInfo.fullyBranchCoveredLineCount;
                        if (childInfo.fullyBranchCoveredLineCount == childInfo.totalLineCount) {
                            result.fullyBranchCoveredFilesCount++;
                        }
                    }
                    result.totalFilesCount++;
                }
            }
            if (result.totalFilesCount == 0) return null;

            String normalizedDirPath = normalizeFilePath(dir.getPath());
            if (isTest) annotator.annotateTestDirectory(normalizedDirPath, result);
            else annotator.annotateSourceDirectory(normalizedDirPath, result);

            return result;
        }
        return null;
    }

    @Override
    protected @Nullable FileCoverageInfoEx fileInfoForCoveredFile(@NotNull ClassData classData) {
        // Adapted from super.fileInfoForCoveredFile
        // class data lines = [0, 1, ... count] but first element with index = #0 is fake and isn't
        // used thus count = length = 1
        final int count = classData.getLines().length - 1;

        if (count == 0) {
            return null;
        }

        final FileCoverageInfoEx info = new FileCoverageInfoEx();
        for (int i = 1; i <= count; i++) {
            final LineData lineData = classData.getLineData(i);
            if (lineData != null) {
                int status = lineData.getStatus();
                if (status != LineCoverage.NONE) {
                    if (status != LineCoverage.PARTIAL) {
                        info.fullyBranchCoveredLineCount++;
                    }
                    info.coveredLineCount++;
                }
            }
            info.totalLineCount++;
        }
        return info;
    }

    @Override
    protected @Nullable FileCoverageInfo fillInfoForUncoveredFile(@NotNull File file) {
        // Explicit null, see {@link #collectFolderCoverageInfo}
        return null;
    }

    // If true, getLinesCoverageInformationString() should return a detailed coverage info (k/n lines).
    // This HACK is needed because myCoverageFileInfos is private and, unlike getDirCoverageInfo, there is no way to
    // access it.
    private enum LinesCoverageRequest {
        DETAILED, BRANCH
    }

    private final ThreadLocal<LinesCoverageRequest> linesCoverageDetailed = new ThreadLocal<>();

    private static String formatPercent(@PropertyKey(resourceBundle = PythonCoverageBundle.BUNDLE) String key, int covered, int total) {
        return PythonCoverageBundle.message(key, covered, total, calcPercent(covered, total));
    }

    @Override
    protected @Nullable @Nls String getLinesCoverageInformationString(@NotNull FileCoverageInfo info) {
        if (info instanceof DirCoverageInfo && ((DirCoverageInfo) info).coveredFilesCount == 0) return null;

        LinesCoverageRequest request = linesCoverageDetailed.get();
        if (request == null) {
            return PythonCoverageBundle.message("viewExtension.coveredLines%Simple",
                    calcPercent(info.coveredLineCount, info.totalLineCount));
        }
        else if (request == LinesCoverageRequest.DETAILED) {
            if (!(info instanceof DirCoverageInfo)) {
                return formatOfTotal(info.coveredLineCount, info.totalLineCount);
            }
            else {
                return formatPercent("viewExtension.coveredLines%", info.coveredLineCount, info.totalLineCount);
            }
        }
        else if (request == LinesCoverageRequest.BRANCH) {
            if (!(info instanceof DirCoverageInfo)) {
                return formatOfTotal(getFullyCoveredLineCount(info), info.totalLineCount);
            }
            else {
                return formatPercent("viewExtension.coveredLines%", getFullyCoveredLineCount(info), info.totalLineCount);
            }
        }
        throw new IllegalArgumentException("Unhandled request " + request);
    }

    private int getFullyCoveredLineCount(@NotNull FileCoverageInfo info) {
        final int fullyCoveredLineCount;
        if ((info instanceof FileCoverageInfoEx)) {
            fullyCoveredLineCount = ((FileCoverageInfoEx) info).fullyBranchCoveredLineCount;
        } else {
            assert info instanceof DirCoverageInfoEx;
            fullyCoveredLineCount = ((DirCoverageInfoEx) info).fullyBranchCoveredLineCount;
        }
        return fullyCoveredLineCount;
    }

    private static String formatOfTotal(int covered, int total) {
        return String.format("%d/%d (%d%%)", covered, total, calcPercent(covered, total));
    }

    @Override
    public @Nullable @Nls String getFilesCoverageInformationString(@NotNull DirCoverageInfo info) {
        if (info.coveredFilesCount > 0) {
            LinesCoverageRequest request = linesCoverageDetailed.get();
            if (request == null) {
                return super.getFilesCoverageInformationString(info);
            } else if (request == LinesCoverageRequest.DETAILED) {
                return formatPercent("viewExtension.coveredFiles%", info.coveredFilesCount, info.totalFilesCount);
            } else if (request == LinesCoverageRequest.BRANCH) {
                return formatPercent("viewExtension.coveredFiles%",
                        ((DirCoverageInfoEx) info).fullyBranchCoveredFilesCount, info.totalFilesCount);
            }
            throw new IllegalArgumentException("Unhandled request " + request);
        } else {
            // For uncovered directories, show the amount of files in them only (covered would be 100%)
            return PythonCoverageBundle.message("viewExtension.noCoveredFiles", info.totalFilesCount);
        }
    }

    private String getCoverageInformationStringRequest(@NotNull LinesCoverageRequest request, @NotNull PsiFileSystemItem fileOrDir, @NotNull CoverageSuitesBundle currentSuite, @NotNull CoverageDataManager manager) {
        linesCoverageDetailed.set(request);
        String result = fileOrDir instanceof PsiFile
                ? getFileCoverageInformationString((PsiFile) fileOrDir, currentSuite, manager)
                : getDirCoverageInformationString((PsiDirectory) fileOrDir, currentSuite, manager);
        linesCoverageDetailed.remove();
        return result;
    }

    @Override
    public @Nullable @Nls String getDetailedCoverageInformationString(@NotNull PsiFileSystemItem fileOrDir, @NotNull CoverageSuitesBundle currentSuite, @NotNull CoverageDataManager manager) {
        return getCoverageInformationStringRequest(LinesCoverageRequest.DETAILED, fileOrDir, currentSuite, manager);
    }

    @Nls
    @Override
    public @Nullable String getBranchCoverageInformationString(@NotNull PsiFileSystemItem fileOrDir, @NotNull CoverageSuitesBundle currentSuite, @NotNull CoverageDataManager manager) {
        return getCoverageInformationStringRequest(LinesCoverageRequest.BRANCH, fileOrDir, currentSuite, manager);
    }

    @Override
    public boolean isCovered(PsiFileSystemItem fileOrDir, CoverageSuitesBundle currentSuite, CoverageDataManager dataManager) {
        if (fileOrDir.isDirectory()) {
            final DirCoverageInfo dirInfo = getDirCoverageInfo((PsiDirectory) fileOrDir, currentSuite);
            return dirInfo != null && dirInfo.coveredFilesCount > 0;
        } else {
            return getFileCoverageInformationString((PsiFile) fileOrDir, currentSuite, dataManager) != null;
        }
    }

    private void updateCoverageViews() {
        isUpdating = false;
        renewVersion++;
        for (Runnable cb : onUpdateViewsHook.values()) {
            cb.run();
        }
        onUpdateViewsHook.clear();
    }

    public void maybeUpdateLater(CoverageView view, Runnable updater) {
        if (isUpdating) {
            onUpdateViewsHook.put(view, updater);
        }
    }

    public boolean isUpdating() {
        return isUpdating;
    }

    public int getRenewVersion() {
        return renewVersion;
    }
}
