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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WonderlandContextSelector {

    private static final Logger LOGGER = LoggerFactory.getLogger(WonderlandContextSelector.class);

    private static final String DEFAULT_SPLIT_REGEX = "\\.";
    private final String splitRegex;

    private WonderlandContextSelector(String splitRegex) {
        this.splitRegex = Objects.requireNonNull(splitRegex, "regex for splitting must not be null");
    }

    public static final WonderlandContextSelector create() {
        return new WonderlandContextSelector(DEFAULT_SPLIT_REGEX);
    }

    public WonderlandContextSelector separatorRegex(String newSplitRegex) {
        return new WonderlandContextSelector(newSplitRegex);
    }

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

    /*
     * TODO: Check that annotations are also detected on methods!
     * TODO: Show non-categorized profiles in a checkbox list
     * TODO: take into account default profiles
     */
    
    public void showSelectionAndSetupSpringProfiles() {
        Collection<String> profiles = new HashSet<>();
        profiles.addAll(new AnnotationProfileFinder().discoverSpringProfilesInDefaultPackages());
        profiles.addAll(new XmlProfileFinder().discoverSpringProfilesInDefaultSelector());

        Set<String> filtered = profiles.stream().map(String::trim).map(s -> {
            if (s.startsWith("!")) {
                if (s.length() > 1) {
                    return Optional.<String> of(s.substring(1));
                } else {
                    return Optional.<String> empty();
                }
            } else {
                return Optional.<String> of(s);
            }
        }).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toSet());

        Map<String, List<String>> profilesByCategory = filtered.stream().map(s -> s.split(splitRegex, 2))
                .filter(s -> s.length == 2).collect(groupingBy(s -> s[0], mapping(s -> s[1], toList())));

        List<ProfileSelectionPanel> selectors = profilesByCategory.entrySet().stream()
                .map(e -> new ProfileSelectionPanel(e.getKey(), e.getValue())).collect(toList());

        JDialog frame = new JDialog();
        frame.setModalityType(ModalityType.APPLICATION_MODAL);
        frame.setTitle("Welcome to the Wonderland!");
        frame.setLayout(new GridLayout(0, 1));
        selectors.forEach(frame::add);
        JButton closeButton = new JButton("Make it so!");
        AtomicBoolean confirmed = new AtomicBoolean(false);
        closeButton.addActionListener(e -> {
            confirmed.set(true);
            frame.setVisible(false);
        });
        frame.add(closeButton);
        frame.setSize(100, 100);
        frame.pack();
        frame.setVisible(true);

        if (!confirmed.get()) {
            LOGGER.info("Selector window closed without confirming. Exiting application.");
            System.exit(0);
        }

        String selectedProfiles = selectors.stream().map(s -> s.getSelectedProfile()).collect(joining(","));
        System.setProperty("spring.profiles.active", selectedProfiles);
    }
}
