package org.nbfalcon.pythonCoverage.util.ideaUtil;

import com.intellij.ui.scale.Scale;
import com.intellij.ui.scale.ScaleContext;
import com.intellij.ui.scale.ScaleType;
import com.intellij.util.IconUtil;

import javax.swing.*;

public class MyIconUtils {
    public static Icon scaleIconTo(Icon icon, int actualWidth, int desiredWidth) {
        final double scaleCoeff = ((double) desiredWidth) / ((double) actualWidth);
        final ScaleContext scale = ScaleContext.create(Scale.create(scaleCoeff, ScaleType.OBJ_SCALE));
        return IconUtil.scale(icon, scale);
    }
}
