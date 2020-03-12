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

package org.ossgang.spring.wonderland;

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
