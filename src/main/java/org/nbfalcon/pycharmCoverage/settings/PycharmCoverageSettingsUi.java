package org.nbfalcon.pycharmCoverage.settings;

import com.intellij.coverage.CoverageOptions;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.jetbrains.python.PythonFileType;
import net.miginfocom.swing.MigLayout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ItemListener;
import java.util.Objects;

public class PycharmCoverageSettingsUi extends CoverageOptions {
    private final @NotNull Project myProject;

    // UI
    private TextFieldWithBrowseButton coveragePyReporterPythonCommand;
    private JBCheckBox coveragePyUseModule;
    private TextFieldWithBrowseButton coveragePyModule;
    private JBCheckBox branchCoverage;

    public PycharmCoverageSettingsUi(@NotNull Project project) {
        this.myProject = project;
    }

    private static @Nullable
    String getTextNull(TextFieldWithBrowseButton field) {
        final String text = field.getText();
        return text.isBlank() ? null : text;
    }

    private static void setTextNull(TextFieldWithBrowseButton field, @Nullable String text) {
        field.setText(text == null ? "" : text);
    }

    @Override
    public JComponent createComponent() {
        // UI
        JPanel mainPanel = new JPanel();
        MigLayout layout = new MigLayout("gap rel 0");
        mainPanel.setLayout(layout);

        coveragePyReporterPythonCommand = new TextFieldWithBrowseButton();
        ((JBTextField) coveragePyReporterPythonCommand.getTextField()).getEmptyText().setText("python");
        coveragePyReporterPythonCommand.addBrowseFolderListener(
                "Select Executable",
                null, myProject,
                FileChooserDescriptorFactory.createSingleFileOrExecutableAppDescriptor(),
                TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);

        coveragePyUseModule = new JBCheckBox("Use custom module:");
        coveragePyModule = new TextFieldWithBrowseButton();
        ((JBTextField) coveragePyModule.getTextField()).getEmptyText().setText("-m coverage");
        coveragePyModule.addBrowseFolderListener(
                "Select Python File",
                null, myProject,
                FileChooserDescriptorFactory.createSingleFileDescriptor(PythonFileType.INSTANCE));
        branchCoverage = new JBCheckBox("Measure branch coverage (if-else)");

        mainPanel.setBorder(IdeBorderFactory.createTitledBorder("Python Coverage"));
        mainPanel.add(new JBLabel("Reporter python command (global):"));
        mainPanel.add(coveragePyReporterPythonCommand, "wrap, pushx, growx");
        mainPanel.add(coveragePyUseModule);
        mainPanel.add(coveragePyModule, "wrap, pushx, growx");
        mainPanel.add(branchCoverage, "wrap");

        final ItemListener coveragePyEnableModuleListener =
                (ignored) -> coveragePyModule.setEnabled(coveragePyUseModule.isSelected());
        coveragePyUseModule.addItemListener(coveragePyEnableModuleListener);
        coveragePyEnableModuleListener.itemStateChanged(null);

        reset();
        return mainPanel;
    }

    @Override
    public boolean isModified() {
        final PycharmCoverageApplicationSettings application = PycharmCoverageApplicationSettings.getInstance();
        final PycharmCoverageProjectSettings project = PycharmCoverageProjectSettings.getInstance(myProject);

        final boolean unmodified = project.coveragePyUseModule == coveragePyUseModule.isSelected()
                && project.enableBranchCoverage == branchCoverage.isSelected()
                && Objects.equals(application.coveragePyLoaderPythonCommand, getTextNull(coveragePyReporterPythonCommand))
                && Objects.equals(project.coveragePyModule, getTextNull(coveragePyModule));
        return !unmodified;
    }

    @Override
    public void apply() {
        final PycharmCoverageApplicationSettings application = PycharmCoverageApplicationSettings.getInstance();
        final PycharmCoverageProjectSettings project = PycharmCoverageProjectSettings.getInstance(myProject);

        application.coveragePyLoaderPythonCommand = getTextNull(coveragePyReporterPythonCommand);
        project.coveragePyModule = getTextNull(coveragePyModule);
        project.coveragePyUseModule = coveragePyUseModule.isSelected();
        project.enableBranchCoverage = branchCoverage.isSelected();
    }

    @Override
    public void reset() {
        final PycharmCoverageApplicationSettings application = PycharmCoverageApplicationSettings.getInstance();
        final PycharmCoverageProjectSettings project = PycharmCoverageProjectSettings.getInstance(myProject);

        setTextNull(coveragePyReporterPythonCommand, application.coveragePyLoaderPythonCommand);
        setTextNull(coveragePyModule, project.coveragePyModule);
        coveragePyUseModule.setSelected(project.coveragePyUseModule);
        branchCoverage.setSelected(project.enableBranchCoverage);
    }
}
