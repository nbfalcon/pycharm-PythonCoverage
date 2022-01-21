package org.nbfalcon.pythonCoverage.coveragePy;

import com.intellij.coverage.CoverageDataManager;
import com.intellij.coverage.CoverageSuitesBundle;
import com.intellij.coverage.SimpleCoverageAnnotator;
import com.intellij.coverage.view.CoverageView;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.util.ui.EdtInvocationManager;
import com.jetbrains.python.PythonFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nbfalcon.pythonCoverage.util.ideaUtil.CoverageViewUpdaterHack;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

// FIXME: We should show actual covered files per dir/uncovered; lines should not be included
// FIXME: Directory: 100% (100/100) lines, 100% (50/50) files
public class CoveragePyAnnotator extends SimpleCoverageAnnotator {
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
        return value instanceof PsiFile
                ? coveragePySupports((PsiFile) value)
                : !Objects.equals(value.getName(), Project.DIRECTORY_STORE_FOLDER);
    }

    private static boolean coveragePySupports(PsiFile file) {
        final FileType fileType = file.getFileType();
        // TODO: Is "Jinja2" actually true for PyCharm Professional?
        return fileType == PythonFileType.INSTANCE || fileType.getName().equals("Jinja2");
    }

    @Override
    protected @Nullable
    Runnable createRenewRequest(@NotNull CoverageSuitesBundle suite, @NotNull CoverageDataManager dataManager) {
        final Runnable renewRequest = super.createRenewRequest(suite, dataManager);
        if (renewRequest == null) return null;

        isUpdating = true;
        return () -> {
            renewRequest.run();
            EdtInvocationManager.getInstance().invokeLater(this::updateCoverageViews);
        };
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
