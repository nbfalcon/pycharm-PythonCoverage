package org.nbfalcon.pycharmCoverage.coveragePy;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.*;
import java.util.List;
import java.util.Map;

public class CoveragePyLoaderJSON {
    public static class CoverageMetadata {
        public String version;
        public String timestamp;

        @SerializedName("branch_coverage")
        public boolean branchCoverage;
        @SerializedName("show_contexts")
        public boolean showContexts;
    }

    public static class Totals {
        public int coveredLines;
        public int numStatements;
        public double percentCovered;
        public String percentCoveredDsiplay;
        public int missingLines;
        public int excludedLines;
    }

    public static class FileData {
        @SerializedName("executed_lines")
        public List<Integer> executedLines;

        public Totals summary;

        @SerializedName("missing_lines")
        public List<Integer> missingLines;
        @SerializedName("excluded_lines")
        public List<Integer> excludedLines;
    }

    public static class CoverageOutput {
        public CoverageMetadata meta;
        public Map<String, FileData> files;
        public Totals totals;
    }

    private static Gson gson;

    public static CoverageOutput loadFromJson(Reader input) {
        return gson.fromJson(input, CoverageOutput.class);
    }
}
