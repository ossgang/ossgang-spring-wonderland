package cern.lhc.spring.wonderland;

import javax.swing.*;
import java.util.Optional;

abstract class ProfileSelector extends JPanel {
    private static final long serialVersionUID = 1L;

    public abstract Optional<String> getSelectedProfile();
}
