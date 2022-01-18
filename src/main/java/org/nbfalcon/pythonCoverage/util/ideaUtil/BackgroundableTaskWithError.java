package org.nbfalcon.pythonCoverage.util.ideaUtil;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class BackgroundableTaskWithError extends Task.Backgroundable {
    public BackgroundableTaskWithError(@Nullable Project project, @NlsContexts.ProgressTitle @NotNull String title, boolean canBeCancelled) {
        super(project, title, canBeCancelled);
    }

    private final Exception[] runException = new Exception[1];

    @Override
    public void run(@NotNull ProgressIndicator progressIndicator) {
        try {
            run1(progressIndicator);
        } catch (Exception e){
            runException[0] = e;
        }
    }

    @Override
    public void onSuccess() {
        if (runException[0] != null) {
            onException(runException[0]);
        }
        else {
            onSuccess1();
        }
    }

    public abstract void run1(@NotNull ProgressIndicator progressIndicator);
    protected abstract void onSuccess1();
    protected abstract void onException(Exception e);
}
