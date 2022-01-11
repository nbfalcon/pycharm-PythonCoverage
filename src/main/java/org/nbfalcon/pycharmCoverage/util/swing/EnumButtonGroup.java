package org.nbfalcon.pycharmCoverage.util.swing;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

public class EnumButtonGroup<E extends Enum<E>> {
    private final ButtonGroup myButtonGroup = new ButtonGroup();

    private final Map<E, ButtonModel> item2Button = new HashMap<>();
    private final Map<ButtonModel, E> button2item = new HashMap<>();

    public void add(E item, AbstractButton button) {
        myButtonGroup.add(button);
        final ButtonModel model = button.getModel();
        item2Button.put(item, model);
        button2item.put(model, item);
    }

    public void add(E item, AbstractButton button, JPanel panel, Object constraints) {
        this.add(item, button);
        panel.add(button, constraints);
    }

    public void add(E item, AbstractButton button, JPanel panel) {
        this.add(item, button);
        panel.add(button);
    }

    public E getSelected() {
        final ButtonModel selected = myButtonGroup.getSelection();
        if (selected == null) return null;

        return button2item.get(selected);
    }

    public void setSelected(E item) {
        ButtonModel whichButton = item2Button.get(item);
        if (whichButton == null)
            throw new IllegalArgumentException("item is not part of EnumButtonGroup");

        myButtonGroup.setSelected(whichButton, true);
    }
}
