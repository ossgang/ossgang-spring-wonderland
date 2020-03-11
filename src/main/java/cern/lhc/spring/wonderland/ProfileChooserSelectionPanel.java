package cern.lhc.spring.wonderland;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

class ProfileChooserSelectionPanel extends ProfileSelector {
    private static final long serialVersionUID = 1L;

    private final String category;
    private final JComboBox<String> profileSelector;
    private final JCheckBox active;

    ProfileChooserSelectionPanel(String category, Collection<String> choices, String defaulSelection) {
        this.category = category;
        boolean isActive = defaulSelection != null;

        setLayout(new BorderLayout());

        JPanel selectionPanel = new JPanel(new BorderLayout());
        selectionPanel.setBorder(BorderFactory.createTitledBorder(category));

        List<String> choiceList = new ArrayList<>(choices);
        choiceList.sort(String::compareTo);
        profileSelector = new JComboBox<>(choiceList.toArray(new String[0]));
        if (isActive) {
            profileSelector.setSelectedItem(defaulSelection);
        }
        profileSelector.setEnabled(isActive);
        selectionPanel.add(profileSelector, BorderLayout.CENTER);

        add(selectionPanel, BorderLayout.CENTER);

        this.active = new JCheckBox();
        this.active.addActionListener(evt -> profileSelector.setEnabled(active.isSelected()));
        this.active.setSelected(isActive);
        add(active, BorderLayout.WEST);
    }

    @Override
    public Optional<String> getSelectedProfile() {
        if (active.isSelected()) {
            return Optional.of(category + '.' + profileSelector.getSelectedItem());
        }
        return Optional.empty();
    }
}
