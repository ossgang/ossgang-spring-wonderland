/**
 * Copyright (c) 2017 European Organisation for Nuclear Research (CERN), All Rights Reserved.
 */

package cern.lhc.spring.wonderland;

import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.Dialog.ModalityType;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class WonderlandContextSelector {

    private static final String SPRING_PROFILES_ACTIVE = "spring.profiles.active";

    private static final Logger LOGGER = LoggerFactory.getLogger(WonderlandContextSelector.class);

    private static final String DEFAULT_SPLIT_REGEX = "\\.";
    private final String splitRegex;
    private final Set<String> collectedProfiles;
    private final Set<String> defaultProfiles;

    private WonderlandContextSelector(String splitRegex, Set<String> defaultProfiles, Set<String> collectedProfiles) {
        this.splitRegex = Objects.requireNonNull(splitRegex, "regex for splitting must not be null");
        requireNonNull(defaultProfiles, "defaultProfiles must not be null");
        this.defaultProfiles = defaultProfiles.stream().map(String::trim).collect(toSet());
        this.collectedProfiles = collectedProfiles;
    }

    public static WonderlandContextSelector create() {
        return new WonderlandContextSelector(DEFAULT_SPLIT_REGEX, ImmutableSet.of(), collectProfiles());
    }

    public WonderlandContextSelector separatorRegex(String newSplitRegex) {
        return new WonderlandContextSelector(newSplitRegex, defaultProfiles, collectedProfiles);
    }

    public WonderlandContextSelector defaultProfiles(String... newDefaultProfiles) {
        return defaultProfiles(ImmutableSet.copyOf(newDefaultProfiles));
    }

    public WonderlandContextSelector defaultProfiles(Set<String> newDefaultProfiles) {
        return new WonderlandContextSelector(splitRegex, newDefaultProfiles, collectedProfiles);
    }

    public WonderlandContextSelector defaultDemoProfiles() {
        Set<String> demoProfiles = collectedProfiles.stream()
                .filter(p -> p.endsWith(".demo"))
                .collect(toSet());
        return new WonderlandContextSelector(splitRegex, demoProfiles, collectedProfiles);
    }

    public void showSelectionIfUnconfigured() {
        String activeProfiles = System.getProperty(SPRING_PROFILES_ACTIVE);
        if (activeProfiles != null) {
            LOGGER.info("property '" + SPRING_PROFILES_ACTIVE + "' is already set. No selection dialog will be shown.");
        } else {
            LOGGER.info("Showing user input dialog for spring profile selection.");
            List<String> selectedProfiles = showProfileSelectionDialog();
            setActiveProfiles(selectedProfiles);
        }
        logActiveProfiles();
    }

    private void setActiveProfiles(List<String> selectedProfiles) {
        LOGGER.info("Setting the following spring profiles as active: " + selectedProfiles);
        System.setProperty(SPRING_PROFILES_ACTIVE, String.join(",", selectedProfiles));
    }

    public void selectProProfiles() {
        List<String> proProfiles = collectedProfiles.stream()
                .filter(p -> p.endsWith(".pro"))
                .collect(toList());
        setActiveProfiles(proProfiles);
    }

    public void selectDemoProfiles() {
        List<String> demoProfiles = collectedProfiles.stream()
                .filter(p -> p.endsWith(".demo"))
                .collect(toList());
        setActiveProfiles(demoProfiles);
    }

    private List<String> showProfileSelectionDialog() {
        Set<String> filtered = collectedProfiles;

        List<String> uncategorizedProfiles = new ArrayList<>();
        Map<String, List<String>> profilesByCategory = new HashMap<>();
        for (String profile : filtered) {
            String[] split = profile.split(splitRegex, 2);
            if (split.length == 2) {
                profilesByCategory.computeIfAbsent(split[0], s -> new ArrayList<>()).add(split[1]);
            } else {
                uncategorizedProfiles.add(profile);
            }
        }

        Set<String> defaultUncategorized = new HashSet<>();
        Map<String, String> defaultCategorized = new HashMap<>();
        for (String profile : defaultProfiles) {
            String[] split = profile.split(splitRegex, 2);
            if (split.length == 2) {
                String category = split[0];
                if (defaultCategorized.containsKey(category)) {
                    throw new IllegalStateException(
                            "More than one default profiles defined for category '" + category + "'!");
                }
                defaultCategorized.put(category, split[1]);
            } else {
                defaultUncategorized.add(profile);
            }
        }

        List<ProfileChooserSelectionPanel> categorySelectors = profilesByCategory.entrySet().stream().map(
                e -> new ProfileChooserSelectionPanel(e.getKey(), e.getValue(), defaultCategorized.get(e.getKey())))
                .collect(toList());

        List<ProfileEnableDisableSelectionPanel> uncategorizedSelectors = uncategorizedProfiles.stream()
                .map(p -> new ProfileEnableDisableSelectionPanel(p, defaultUncategorized.contains(p)))
                .collect(toList());

        JDialog frame = new JDialog();
        frame.setModalityType(ModalityType.APPLICATION_MODAL);
        frame.setTitle("Welcome to the Wonderland!");
        frame.setLayout(new GridLayout(0, 1, 5, 5));
        categorySelectors.forEach(frame::add);
        uncategorizedSelectors.forEach(frame::add);
        JButton closeButton = new JButton("Make it so!");
        AtomicBoolean confirmed = new AtomicBoolean(false);
        closeButton.addActionListener(e -> {
            confirmed.set(true);
            frame.setVisible(false);
        });
        frame.add(closeButton);
        frame.setMinimumSize(new Dimension(400, 300));
        frame.pack();
        frame.setVisible(true);

        if (!confirmed.get()) {
            LOGGER.info("Selector window closed without confirming. Exiting application.");
            System.exit(0);
        }

        return categorySelectors.stream()
                .map(ProfileChooserSelectionPanel::getSelectedProfile)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toList());
    }

    private static Set<String> collectProfiles() {
        LOGGER.info("Collecting all Spring profiles");
        Collection<String> profiles = new HashSet<>();
        profiles.addAll(new AnnotationProfileFinder().discoverSpringProfilesInDefaultPackages());
        profiles.addAll(new XmlProfileFinder().discoverSpringProfilesInDefaultSelector());

        return profiles.stream().map(String::trim).map(s -> {
            if (s.startsWith("!")) {
                if (s.length() > 1) {
                    return Optional.of(s.substring(1));
                } else {
                    return Optional.<String>empty();
                }
            } else {
                return Optional.of(s);
            }
        }).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toSet());
    }

    private void logActiveProfiles() {
        LOGGER.info(SPRING_PROFILES_ACTIVE + "=" + System.getProperty(SPRING_PROFILES_ACTIVE));
    }
}
