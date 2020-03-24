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

import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import org.springframework.context.annotation.Profile;

import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Predicates.not;

public class AnnotationProfileFinder {

    public Set<String> discoverSpringProfilesIn(Collection<String> prefixes) {
        try {
            Collection<Class<?>> matchedClasses = ClassPath.from(ClassLoader.getSystemClassLoader()).getAllClasses()
                    .stream().filter(classInfo -> matchesAnyPrefix(classInfo.getPackageName(), prefixes))
                    .map(tryOptional(ClassInfo::load)).filter(Optional::isPresent).map(Optional::get)
                    .collect(Collectors.toList());

            List<Method> matchedMethods = matchedClasses.stream().flatMap(this::getMethodsFromClass)
                    .collect(Collectors.toList());

            Set<String> profiles = new HashSet<>();
            profiles.addAll(profilesFromAnnotations(matchedClasses));
            profiles.addAll(profilesFromAnnotations(matchedMethods));
            return profiles;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Stream<? extends Method> getMethodsFromClass(Class<?> clazz) {
        try {
            return Stream.of(clazz.getDeclaredMethods());
        } catch (NoClassDefFoundError e) {
            return Stream.empty();
        }
    }

    private Set<String> profilesFromAnnotations(Collection<? extends AnnotatedElement> annotations) {
        return annotations.stream().map(element -> element.getAnnotation(Profile.class))
                .filter(not(Objects::isNull))
                .flatMap(annotation -> Stream.of(annotation.value()))
                .collect(Collectors.toSet());
    }

    private static boolean matchesAnyPrefix(String txt, Collection<String> prefixes) {
        return prefixes.stream().anyMatch(txt::startsWith);
    }

    private static <I, R> Function<I, Optional<R>> tryOptional(Function<I, R> mapper) {
        return i -> {
            try {
                return Optional.ofNullable(mapper.apply(i));
            } catch (NoClassDefFoundError e) {
                return Optional.empty();
            }
        };
    }

}
