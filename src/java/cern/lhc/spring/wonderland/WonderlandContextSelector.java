/**
 * Copyright (c) 2017 European Organisation for Nuclear Research (CERN), All Rights Reserved.
 */

package cern.lhc.spring.wonderland;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.awt.BorderLayout;
import java.awt.Dialog.ModalityType;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;

public class WonderlandContextSelector {

    private static class ProfileSelectionPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        private final String category;
        private final JComboBox<String> profileSelector;

        ProfileSelectionPanel(String category, Collection<String> choices) {
            this.category = category;
            setBorder(BorderFactory.createTitledBorder(category));
            setLayout(new BorderLayout());

            List<String> choiceList = new ArrayList<>(choices);
            choiceList.sort(String::compareTo);
            profileSelector = new JComboBox<>(choiceList.toArray(new String[0]));
            add(profileSelector, BorderLayout.CENTER);
        }

        public String getSelectedProfile() {
            return category + '.' + profileSelector.getSelectedItem();
        }
    }

    public static void showSelectionAndSetupSpringProfiles() {
        Collection<String> profiles = new HashSet<>();
        profiles.addAll(new AnnotationProfileFinder().discoverSpringProfilesInDefaultPackages());
        profiles.addAll(new XmlProfileFinder().discoverSpringProfilesInDefaultSelector());

        Map<String, List<String>> profilesByCategory = profiles.stream().map(s -> s.split("\\.", 2))
                .filter(s -> s.length == 2).collect(groupingBy(s -> s[0], mapping(s -> s[1], toList())));

        List<ProfileSelectionPanel> selectors = profilesByCategory.entrySet().stream()
                .map(e -> new ProfileSelectionPanel(e.getKey(), e.getValue())).collect(toList());

        JDialog frame = new JDialog();
        frame.setModalityType(ModalityType.APPLICATION_MODAL);
        frame.setTitle("Welcome to the Wonderland!");
        frame.setLayout(new GridLayout(0, 1));
        selectors.forEach(frame::add);
        JButton closeButton = new JButton("Make it so!");
        closeButton.addActionListener(e -> frame.setVisible(false));
        frame.add(closeButton);
        frame.setSize(100, 100);
        frame.pack();
        frame.setVisible(true);

        String selectesProfiles = selectors.stream().map(s -> s.getSelectedProfile()).collect(joining(","));
        System.setProperty("spring.profiles.active", selectesProfiles);
    }
}
