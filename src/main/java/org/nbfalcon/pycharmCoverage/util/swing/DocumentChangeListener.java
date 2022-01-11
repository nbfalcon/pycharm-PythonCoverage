package org.nbfalcon.pycharmCoverage.util.swing;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public abstract class DocumentChangeListener implements DocumentListener {
    public abstract void onDocumentChange(DocumentEvent e);

    @Override
    public void insertUpdate(DocumentEvent e) {
        onDocumentChange(e);
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        onDocumentChange(e);
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        onDocumentChange(e);
    }
}
