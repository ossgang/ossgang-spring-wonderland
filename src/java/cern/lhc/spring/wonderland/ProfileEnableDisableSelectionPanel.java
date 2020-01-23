package cern.lhc.spring.wonderland;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;

class ProfileEnableDisableSelectionPanel extends ProfileSelector {
    private static final long serialVersionUID = 1L;

    private final String profileName;
    private final JCheckBox active;

    ProfileEnableDisableSelectionPanel(String profile, boolean enabled) {
        this.profileName = profile;
        setLayout(new BorderLayout());

        JLabel profileNameLabel = new JLabel(profile);
        profileNameLabel.setEnabled(enabled);

        add(profileNameLabel, BorderLayout.CENTER);

        this.active = new JCheckBox();
        this.active.setSelected(enabled);
        this.active.addActionListener(evt -> profileNameLabel.setEnabled(active.isSelected()));
        add(active, BorderLayout.WEST);
    }

    @Override
    public Optional<String> getSelectedProfile() {
        if (active.isSelected()) {
            return Optional.of(profileName);
        }
        return Optional.empty();
    }
}
