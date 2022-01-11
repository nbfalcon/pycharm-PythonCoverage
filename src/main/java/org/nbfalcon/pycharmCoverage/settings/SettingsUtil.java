package org.nbfalcon.pycharmCoverage.settings;

import org.nbfalcon.pycharmCoverage.util.ShellArgumentTokenizer;

import java.util.Arrays;
import java.util.List;

public class SettingsUtil {
    public static ProcessBuilder createProcess(String baseCommandLine, String... args) {
        final List<String> baseArgs = ShellArgumentTokenizer.tokenize(baseCommandLine);
        baseArgs.addAll(Arrays.asList(args));
        return new ProcessBuilder(baseCommandLine);
    }
}
