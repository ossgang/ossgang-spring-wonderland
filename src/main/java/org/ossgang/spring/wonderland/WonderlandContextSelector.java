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

import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.Dialog.ModalityType;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class WonderlandContextSelector {

    private static final String SPRING_PROFILES_ACTIVE = "spring.profiles.active";

    private static final Logger LOGGER = LoggerFactory.getLogger(WonderlandContextSelector.class);

    private static final List<String> DEFAULT_SCAN_LOCATIONS = asList("mpe", "cern", "classpath*:**/wonderland-*.xml");
    private static final String CATEGORY_SEPARATOR = ".";
    private static final String SPLIT_REGEX = "\\.";
    private static final String DEFAULT_DEMO_PREFIX = "demo";
    private static final String DEFAULT_PRO_PREFIX = "pro";
    private final Set<String> collectedProfiles;
    private final Set<String> defaultProfiles;
    private final boolean allowDisablingCategories;

    private WonderlandContextSelector(Set<String> defaultProfiles, Set<String> collectedProfiles, boolean allowDisablingCategories) {
        requireNonNull(defaultProfiles, "defaultProfiles must not be null");
        this.defaultProfiles = defaultProfiles.stream().map(String::trim).collect(toSet());
        this.collectedProfiles = collectedProfiles;
        this.allowDisablingCategories = allowDisablingCategories;
    }

    public static WonderlandContextSelector create() {
        return create(DEFAULT_SCAN_LOCATIONS);
    }

    public static WonderlandContextSelector create(String... profileScanLocations) {
        return create(asList(profileScanLocations));
    }

    public static WonderlandContextSelector create(Collection<String> profileScanLocations) {
        List<WonderlandProfileFinder> finders = asList(new AnnotationProfileFinder(), new XmlProfileFinder());
        return new WonderlandContextSelector(ImmutableSet.of(), collectProfiles(profileScanLocations, finders), true);
    }

    public static WonderlandContextSelector create(Collection<String> profileScanLocations, Collection<WonderlandProfileFinder> finders) {
        return new WonderlandContextSelector(ImmutableSet.of(), collectProfiles(profileScanLocations, finders), true);
    }

    public WonderlandContextSelector defaultProfiles(String... newDefaultProfiles) {
        return defaultProfiles(ImmutableSet.copyOf(newDefaultProfiles));
    }

    public WonderlandContextSelector defaultProfiles(Set<String> newDefaultProfiles) {
        return new WonderlandContextSelector(newDefaultProfiles, collectedProfiles, allowDisablingCategories);
    }

    public WonderlandContextSelector defaultProfilesWithPrefix(String prefix) {
        Set<String> filteredProfiles = collectedProfiles.stream()
                .filter(p -> p.contains(CATEGORY_SEPARATOR + prefix))
                .collect(toSet());
        return new WonderlandContextSelector(filteredProfiles, collectedProfiles, allowDisablingCategories);
    }

    public WonderlandContextSelector defaultDemoProfiles() {
        return defaultProfilesWithPrefix(DEFAULT_DEMO_PREFIX);
    }

    public WonderlandContextSelector defaultProProfiles() {
        return defaultProfilesWithPrefix(DEFAULT_PRO_PREFIX);
    }

    public WonderlandContextSelector withoutDisablingOfCategories() {
        return new WonderlandContextSelector(defaultProfiles, collectedProfiles, false);
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

    public void selectProfilesWithPrefix(String prefix) {
        List<String> filteredProfiles = collectedProfiles.stream()
                .filter(p -> p.contains(CATEGORY_SEPARATOR + prefix))
                .collect(toList());
        setActiveProfiles(filteredProfiles);
    }

    public void selectProProfiles() {
        selectProfilesWithPrefix(DEFAULT_PRO_PREFIX);
    }

    public void selectDemoProfiles() {
        selectProfilesWithPrefix(DEFAULT_DEMO_PREFIX);
    }

    private List<String> showProfileSelectionDialog() {
        Set<String> filtered = collectedProfiles;

        List<String> uncategorizedProfiles = new ArrayList<>();
        Map<String, List<String>> profilesByCategory = new HashMap<>();
        for (String profile : filtered) {
            String[] split = profile.split(SPLIT_REGEX, 2);
            if (split.length == 2) {
                profilesByCategory.computeIfAbsent(split[0], s -> new ArrayList<>()).add(split[1]);
            } else {
                uncategorizedProfiles.add(profile);
            }
        }

        Set<String> defaultUncategorized = new HashSet<>();
        Map<String, String> defaultCategorized = new HashMap<>();
        for (String profile : defaultProfiles) {
            String[] split = profile.split(SPLIT_REGEX, 2);
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
                e -> new ProfileChooserSelectionPanel(e.getKey(), e.getValue(), defaultCategorized.get(e.getKey()),
                        allowDisablingCategories))
                .collect(toList());

        List<ProfileEnableDisableSelectionPanel> uncategorizedSelectors = Collections.emptyList();
        if (allowDisablingCategories) {
            uncategorizedSelectors = uncategorizedProfiles.stream()
                    .map(p -> new ProfileEnableDisableSelectionPanel(p, defaultUncategorized.contains(p)))
                    .collect(toList());
        }

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

    private static Set<String> collectProfiles(Collection<String> profileScanLocations, Collection<WonderlandProfileFinder> finders) {
        LOGGER.info("Collecting all Spring profiles");
        Collection<String> profiles = finders.stream()
                .flatMap(f -> f.discoverSpringProfilesIn(profileScanLocations).stream())
                .collect(Collectors.toSet());

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
