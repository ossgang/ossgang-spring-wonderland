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

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.BeanDefinitionDocumentReader;
import org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.w3c.dom.Element;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class XmlProfileFinder implements WonderlandProfileFinder {

    @Override
    public Set<String> discoverSpringProfilesIn(Collection<String> scanLocations) {
        Set<String> discoveredProfiles = new HashSet<>();
        for (String selectorUrl : scanLocations) {
            if (!selectorUrl.startsWith("classpath*:")) {
                continue;
            }
            ProfileListExtractingDocumentReader documentReader = new ProfileListExtractingDocumentReader();
            CustomXmlBeamDefinitionReader reader = new CustomXmlBeamDefinitionReader(documentReader);
            reader.loadBeanDefinitions(selectorUrl);
            discoveredProfiles.addAll(documentReader.getProfiles());
        }
        return discoveredProfiles;
    }

    private static class ProfileListExtractingDocumentReader extends DefaultBeanDefinitionDocumentReader {
        private Set<String> profiles = new HashSet<>();

        @Override
        protected void doRegisterBeanDefinitions(Element element) {
            String profile = element.getAttribute(PROFILE_ATTRIBUTE);
            if (!profile.isEmpty()) {
                String[] newProfiles = profile.split("\\s*,\\s*");
                synchronized (profiles) {
                    profiles.addAll(Arrays.asList(newProfiles));
                }
            }
            super.doRegisterBeanDefinitions(element);
        }

        public Set<String> getProfiles() {
            return profiles;
        }

    }

    private static class CustomXmlBeamDefinitionReader extends XmlBeanDefinitionReader {

        private final DefaultBeanDefinitionDocumentReader documentReader;

        public CustomXmlBeamDefinitionReader(DefaultBeanDefinitionDocumentReader documentReader) {
            super(new DefaultListableBeanFactory());
            this.documentReader = documentReader;
        }

        @Override
        protected BeanDefinitionDocumentReader createBeanDefinitionDocumentReader() {
            return documentReader;
        }

    }
}
