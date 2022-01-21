package org.nbfalcon.pythonCoverage.coveragePy;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.LineCoverage;
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.ProjectData;
import jetbrains.coverage.report.*;
import jetbrains.coverage.report.impl.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CoveragePyHTMLData implements CoverageData, CoverageSourceData {
    private static final Logger LOG = Logger.getInstance(CoveragePyHTMLData.class);

    private @NotNull
    final SourceCodeProvider codeProvider;
    private final List<ClassInfo> myClasses;

    public CoveragePyHTMLData(@NotNull ProjectData data, @NotNull SourceCodeProvider codeProvider) {
        this.codeProvider = codeProvider;
        this.myClasses = computeClassInfo(data);
    }

    private static CoverageStatus lineCoverage2Status(byte lineCoverage) {
        if (lineCoverage == LineCoverage.NONE) return CoverageStatus.NONE;
        else if (lineCoverage == LineCoverage.PARTIAL) return CoverageStatus.PARTIAL;
        else return CoverageStatus.FULL;
    }

    @NotNull
    private static List<ClassInfo> computeClassInfo(ProjectData data) {
        List<ClassInfo> infos = new ArrayList<>();
        for (ClassData clazz : data.getClasses().values()) {
            infos.add(new MyClassInfo(clazz));
        }
        return infos;
    }

    @NotNull
    @Override
    public Collection<ClassInfo> getClasses() {
        return myClasses;
    }

    @Nullable
    @Override
    public CoverageSourceData getSourceData() {
        return this;
    }

    private @Nullable CharSequence getSourceCodeNoexcept(String fqName) {
        CharSequence sourceCode = null;
        try {
            sourceCode = codeProvider.getSourceCode(fqName);
            if (sourceCode == null) {
                LOG.error("Error: could not open file: " + fqName);
            }
        } catch (IOException e) {
            LOG.error(e);
        }
        return sourceCode;
    }

    @Override
    public void renderSourceCodeFor(@NotNull ClassInfo clazz, @NotNull CoverageCodeRenderer renderer) {
        final CharSequence sourceCode = getSourceCodeNoexcept(clazz.getFQName());
        if (sourceCode != null) {
            final MyClassInfo myClazz = (MyClassInfo) clazz;
            int lineNo = 1;
            for (CharSequence line : StringUtil.getLines(sourceCode)) {
                final LineData lineData = myClazz.data.getLineData(lineNo - 1);
                final @Nullable CoverageStatus status = (lineData != null)
                        ? lineCoverage2Status((byte) lineData.getStatus())
                        : null;
                renderer.writeCodeLine(lineNo, line, status);
                lineNo++;
            }
            renderer.codeWriteFinished();
        }
    }

    private static class MyClassInfo implements ClassInfo {
        final ClassData data;

        public MyClassInfo(ClassData data) {
            this.data = data;
        }

        @NotNull
        @Override
        public String getName() {
            return new File(getFQName()).getName();
        }

        @Nullable
        @Override
        public String getModule() {
            return null;
        }

        @NotNull
        @Override
        public String getNamespace() {
            return new File(getFQName()).getParent();
        }

        @NotNull
        @Override
        public String getFQName() {
            return data.getName();
        }

        @Nullable
        @Override
        public Collection<ClassInfo> getInnerClasses() {
            return null;
        }

        @Nullable
        @Override
        public Entry getMethodStats() {
            return null;
        }

        @Nullable
        @Override
        public Entry getBlockStats() {
            return null;
        }

        @Nullable
        @Override
        public Entry getLineStats() {
            int covered = 0;
            int totalLines = 0;
            for (Object line : data.getLines()) {
                if (line != null) {
                    if (((LineData) line).getStatus() != LineCoverage.NONE) covered++;
                    totalLines++;
                }
            }
            return new Entry(totalLines, covered);
        }

        @Nullable
        @Override
        public Entry getStatementStats() {
            return null;
        }
    }
}
