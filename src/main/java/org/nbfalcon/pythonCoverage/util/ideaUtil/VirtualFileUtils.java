package org.nbfalcon.pythonCoverage.util.ideaUtil;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.MalformedInputException;

public class VirtualFileUtils {
    /**
     * Read a virtual vfile as a string.
     *
     * @param vfile The vfile to read to a string.
     * @return The vfile's contents as a string.
     */
    public static @NotNull
    String readVirtualFile(@NotNull VirtualFile vfile) throws IOException {
        byte[] content;
        content = vfile.contentsToByteArray(true);

        // This cannot throw: decode in String() is called with doReplace, which causes coding errors to be swallowed.
        return new String(content, vfile.getCharset());
        // try {
        //     return new String(content, vfile.getCharset());
        // } catch (IllegalArgumentException e) {
        //     throw (MalformedInputException)e.getCause();
        // }
    }
}
