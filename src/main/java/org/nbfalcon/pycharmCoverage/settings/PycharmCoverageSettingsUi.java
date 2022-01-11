package org.nbfalcon.pycharmCoverage.settings;

import com.intellij.coverage.CoverageOptions;
import com.intellij.openapi.project.Project;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.ui.components.JBTextField;
import net.miginfocom.swing.MigLayout;
import org.jetbrains.annotations.NotNull;
import org.nbfalcon.pycharmCoverage.util.swing.DocumentChangeListener;
import org.nbfalcon.pycharmCoverage.util.swing.EnumButtonGroup;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.event.ItemListener;

public class PycharmCoverageSettingsUi extends CoverageOptions {
    private final @NotNull Project myProject;

    // UI
    private JBTextField coveragePyReporterPythonCommand;
    private EnumButtonGroup<PycharmCoverageProjectSettings.WhichRunner> coveragePyWhichRunner;
    private JBTextField coveragePyModule;

    private boolean myIsModified = false;

    public PycharmCoverageSettingsUi(@NotNull Project project) {
        this.myProject = project;
    }

    @Override
    public JComponent createComponent() {
        // UI
        JPanel mainPanel = new JPanel();
        MigLayout layout = new MigLayout("gap rel 0");
        mainPanel.setLayout(layout);

        mainPanel.setBorder(IdeBorderFactory.createTitledBorder("Python Coverage"));
        mainPanel.add(new JBLabel("Reporter python command (global):"));
        mainPanel.add(coveragePyReporterPythonCommand = new JBTextField("python"), "wrap, pushx, growx");

        final JBRadioButton coveragePyUseBuiltin = new JBRadioButton("Use built-in coverage.py");
        final JBRadioButton coveragePyUseCustom = new JBRadioButton("Use custom module:");
        coveragePyWhichRunner = new EnumButtonGroup<>();
        coveragePyWhichRunner.add(
                PycharmCoverageProjectSettings.WhichRunner.BUILT_IN,
                coveragePyUseBuiltin,
                mainPanel, "wrap");
        coveragePyWhichRunner.add(
                PycharmCoverageProjectSettings.WhichRunner.CUSTOM,
                coveragePyUseCustom,
                mainPanel
        );

        mainPanel.add(coveragePyModule = new JBTextField("-m coverage"), "wrap, pushx, growx");
        final ItemListener coveragePyUseCustomListener = (ignored) -> {
            boolean selected = coveragePyUseCustom.isSelected();
            coveragePyModule.setEnabled(selected);
        };
        coveragePyUseCustom.addItemListener(coveragePyUseCustomListener);
        coveragePyWhichRunner.setSelected(PycharmCoverageProjectSettings.WhichRunner.BUILT_IN);
        coveragePyUseCustomListener.itemStateChanged(null);

        modifiedListener(coveragePyUseBuiltin);
        modifiedListener(coveragePyUseCustom);
        modifiedListener(coveragePyReporterPythonCommand);
        modifiedListener(coveragePyModule);

        return mainPanel;
    }

    @Override
    public boolean isModified() {
        return myIsModified;
    }

    @Override
    public void apply() {
        final PycharmCoverageApplicationSettings application = PycharmCoverageApplicationSettings.getInstance();
        final PycharmCoverageProjectSettings project = PycharmCoverageProjectSettings.getInstance(myProject);

        application.coveragePyLoaderPythonCommand = coveragePyReporterPythonCommand.getText();
        project.coveragePyWhichRunner = coveragePyWhichRunner.getSelected();
        project.coveragePyModule = coveragePyModule.getText();

        myIsModified = false;
    }

    @Override
    public void reset() {
        final PycharmCoverageApplicationSettings application = PycharmCoverageApplicationSettings.getInstance();
        final PycharmCoverageProjectSettings project = PycharmCoverageProjectSettings.getInstance(myProject);

        application.coveragePyLoaderPythonCommand = coveragePyReporterPythonCommand.getText();
        coveragePyWhichRunner.setSelected(project.coveragePyWhichRunner);
        coveragePyModule.setText(project.coveragePyModule);

        myIsModified = false;
    }

    private void setModified() {
        myIsModified = true;
    }

    private void modifiedListener(AbstractButton button) {
        button.addItemListener(e -> {
            if (button.isSelected()) setModified();
        });
    }

    private void modifiedListener(JTextField field) {
        field.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            public void onDocumentChange(DocumentEvent e) {
                setModified();
            }
        });
    }
}
