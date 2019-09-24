/**
 * Copyright (c) 2017 European Organisation for Nuclear Research (CERN), All Rights Reserved.
 */

package cern.lhc.spring.wonderland;

import static com.google.common.base.Predicates.isNull;
import static com.google.common.base.Predicates.not;

import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.context.annotation.Profile;

import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;

public class AnnotationProfileFinder {

    public Collection<String> discoverSpringProfilesIn(Collection<String> packages) {
        List<String> profiles = new ArrayList<>();
        profiles.addAll(getConfigurationClassAnnotations(packages));
        return profiles;
    }

    private Collection<String> getConfigurationClassAnnotations(Collection<String> prefixes) {

        try {
            Collection<Class<?>> matchedClasses = ClassPath.from(ClassLoader.getSystemClassLoader()).getAllClasses()
                    .stream().filter(classInfo -> matchesAnyPrefix(classInfo.getPackageName(), prefixes))
                    .map(tryOptional(ClassInfo::load)).filter(Optional::isPresent).map(Optional::get)
                    .collect(Collectors.toList());

            List<Method> matchedMethods = matchedClasses.stream().flatMap(this::getMethodsFromClass)
                    .collect(Collectors.toList());

            Collection<String> profiles = new HashSet<>();
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
        return annotations.stream().map(element -> element.getAnnotation(Profile.class)).filter(not(isNull()))
                .flatMap(annotation -> Stream.of(annotation.value())).collect(Collectors.toSet());
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
