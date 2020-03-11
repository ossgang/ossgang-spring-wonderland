/*
 * @formatter:off
 * Copyright (c) 2008-2020, CERN. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * @formatter:on
 */

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
