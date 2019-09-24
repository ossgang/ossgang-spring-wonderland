/**
 * Copyright (c) 2017 European Organisation for Nuclear Research (CERN), All Rights Reserved.
 */

package cern.lhc.spring.wonderland;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.BeanDefinitionDocumentReader;
import org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.w3c.dom.Element;

public class XmlProfileFinder {
    private static final String DEFAULT_XML_URL_SELECTOR = "classpath*:**/wonderland-*.xml";

    public Set<String> discoverSpringProfilesIn(String selectorUrl) {
        ProfileListExtractingDocumentReader documentReader = new ProfileListExtractingDocumentReader();
        CustomXmlBeamDefinitionReader reader = new CustomXmlBeamDefinitionReader(documentReader);
        reader.loadBeanDefinitions(selectorUrl);
        return documentReader.getProfiles();

    }

    public Set<String> discoverSpringProfilesInDefaultSelector() {
        return discoverSpringProfilesIn(DEFAULT_XML_URL_SELECTOR);
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
