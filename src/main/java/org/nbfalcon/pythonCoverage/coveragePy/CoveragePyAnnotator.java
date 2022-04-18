package org.nbfalcon.pythonCoverage.coveragePy;

import com.intellij.coverage.CoverageDataManager;
import com.intellij.coverage.CoverageEngine;
import com.intellij.coverage.CoverageSuitesBundle;
import com.intellij.coverage.SimpleCoverageAnnotator;
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
    protected @Nullable DirCoverageInfo collectFolderCoverage(@NotNull VirtualFile dir, @NotNull CoverageDataManager dataManager, Annotator annotator, ProjectData projectInfo, boolean trackTestFolders, @NotNull ProjectFileIndex index, @NotNull CoverageEngine coverageEngine, Set<? super VirtualFile> visitedDirs, @NotNull Map<String, String> normalizedFiles2Files) {
        if (!visitedDirs.add(dir)) return null;

        final Boolean indexOk = ReadAction.compute(() -> index.isInContent(dir) && (shouldCollectCoverageInsideLibraryDirs() || !index.isInLibrary(dir)));
        if (!indexOk) return null;

        final boolean isTest = TestSourcesFilter.isTestSources(dir, getProject());
        if (isTest && !trackTestFolders) return null;

        final VirtualFile[] children = dataManager.doInReadActionIfProjectOpen(dir::getChildren);
        if (children != null) {
            DirCoverageInfo result = new DirCoverageInfo();
            for (VirtualFile child : children) {
                if (child.isDirectory()) {
                    final DirCoverageInfo childInfo = collectFolderCoverage(child, dataManager, annotator, projectInfo, trackTestFolders, index, coverageEngine, visitedDirs, normalizedFiles2Files);
                    if (childInfo != null) {
                        result.totalFilesCount += childInfo.totalFilesCount;
                        result.coveredFilesCount += childInfo.coveredFilesCount;
                        result.totalLineCount += childInfo.totalLineCount;
                        result.coveredLineCount += childInfo.coveredLineCount;
                    }
                } else if (coverageEngine.coverageProjectViewStatisticsApplicableTo(child)) {
                    final FileCoverageInfo childInfo = collectBaseFileCoverage(child, annotator, projectInfo, normalizedFiles2Files);
                    if (childInfo != null) {
                        result.totalLineCount += childInfo.totalLineCount;
                        result.coveredLineCount += childInfo.coveredLineCount;

                        result.coveredFilesCount++;
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
    protected @Nullable FileCoverageInfo fillInfoForUncoveredFile(@NotNull File file) {
        // Explicit null, see {@link #collectFolderCoverageInfo}
        return null;
    }

    // If true, getLinesCoverageInformationString() should return a detailed coverage info (k/n lines).
    // This HACK is needed because myCoverageFileInfos is private and, unlike getDirCoverageInfo, there is no way to
    // access it.
    private final ThreadLocal<Boolean> linesCoverageDetailed = new ThreadLocal<>();

    private boolean isLinesCoverageDetailed() {
        final Boolean isDetailed = linesCoverageDetailed.get();
        return isDetailed != null && isDetailed;
    }

    private static String formatPercent(@PropertyKey(resourceBundle = PythonCoverageBundle.BUNDLE) String key, int covered, int total) {
        return PythonCoverageBundle.message(key, covered, total, calcPercent(covered, total));
    }

    @Override
    protected @Nullable @Nls String getLinesCoverageInformationString(@NotNull FileCoverageInfo info) {
        if (info instanceof DirCoverageInfo && ((DirCoverageInfo) info).coveredFilesCount == 0) return null;

        if (isLinesCoverageDetailed()) {
            return formatPercent("viewExtension.coveredLines%", info.coveredLineCount, info.totalLineCount);
        } else {
            return PythonCoverageBundle.message("viewExtension.coveredLines%Simple",
                    calcPercent(info.coveredLineCount, info.totalLineCount));
        }
    }

    @Override
    public @Nullable @Nls String getFilesCoverageInformationString(@NotNull DirCoverageInfo info) {
        if (info.coveredFilesCount > 0) {
            if (isLinesCoverageDetailed()) {
                return formatPercent("viewExtension.coveredFiles%", info.coveredFilesCount, info.totalFilesCount);
            } else {
                return super.getFilesCoverageInformationString(info);
            }
        } else {
            // For uncovered directories, show the amount of files in them only (covered would be 100%)
            return PythonCoverageBundle.message("viewExtension.noCoveredFiles", info.totalFilesCount);
        }
    }

    @Override
    public @Nullable @Nls String getDetailedCoverageInformationString(@NotNull PsiFileSystemItem fileOrDir, @NotNull CoverageSuitesBundle currentSuite, @NotNull CoverageDataManager manager) {
        linesCoverageDetailed.set(true);
        String result = fileOrDir instanceof PsiFile ? getFileCoverageInformationString((PsiFile) fileOrDir, currentSuite, manager) : getDirCoverageInformationString((PsiDirectory) fileOrDir, currentSuite, manager);
        linesCoverageDetailed.set(false);
        return result;
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
