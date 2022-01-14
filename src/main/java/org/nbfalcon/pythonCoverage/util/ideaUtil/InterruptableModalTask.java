package org.nbfalcon.pythonCoverage.util.ideaUtil;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.progress.util.AbstractProgressIndicatorExBase;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.wm.ex.ProgressIndicatorEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nbfalcon.pythonCoverage.util.ThrowingCallable;

import java.util.concurrent.atomic.AtomicReference;

public abstract class InterruptableModalTask extends Task.Modal {
    public InterruptableModalTask(@Nullable Project project, @NlsContexts.DialogTitle @NotNull String title) {
        super(project, title, true);
    }

    @SuppressWarnings("unchecked")
    public static <T, E extends Exception> T runSyncForResult(
            @Nullable Project project, @NlsContexts.DialogTitle @NotNull String title,
            ThrowingCallable<T, E> task) throws E {
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> exception = new AtomicReference<>();
        ProgressManager.getInstance().run(new InterruptableModalTask(project, title) {
            @Override
            public void runInterruptable(@NotNull ProgressIndicator progressIndicator) {
                try {
                    result.set(task.call());
                } catch (Throwable e) {
                    exception.set(e);
                }
            }
        });
        if (exception.get() != null) throw (E) exception.get();
        return result.get();
    }

    @Override
    public void run(@NotNull ProgressIndicator progressIndicator) {
        Thread runThread = Thread.currentThread();

        // This is the case for actual uses of run; we need to use a state delegate, because onCancel is called only
        // after run (which doesn't make much sense...)
        if (progressIndicator instanceof ProgressIndicatorEx) {
            ((ProgressIndicatorEx) progressIndicator).addStateDelegate(new AbstractProgressIndicatorExBase() {
                @Override
                public void cancel() {
                    runThread.interrupt();
                }
            });
        }
        runInterruptable(progressIndicator);
    }

    public abstract void runInterruptable(@NotNull ProgressIndicator progressIndicator);
}