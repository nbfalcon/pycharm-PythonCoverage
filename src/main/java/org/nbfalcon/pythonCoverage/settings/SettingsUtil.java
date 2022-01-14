package org.nbfalcon.pythonCoverage.settings;

import org.nbfalcon.pythonCoverage.util.ShellArgumentTokenizer;

import java.util.Arrays;
import java.util.List;

public class SettingsUtil {
    public static ProcessBuilder createProcess(String baseCommandLine, String... args) {
        final List<String> baseArgs = ShellArgumentTokenizer.tokenize(baseCommandLine);
        baseArgs.addAll(Arrays.asList(args));
        return new ProcessBuilder(baseArgs);
    }

    public static boolean shellArgsIsBlank(String baseCommandLine) {
        return ShellArgumentTokenizer.tokenize(baseCommandLine).isEmpty();
    }
}
