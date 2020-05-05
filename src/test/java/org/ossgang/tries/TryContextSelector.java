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

package org.ossgang.tries;

import com.google.common.collect.ImmutableSet;
import org.ossgang.spring.wonderland.WonderlandContextSelector;
import org.ossgang.spring.wonderland.WonderlandProfileFinder;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class TryContextSelector {

    public static void main(String[] args) {
        WonderlandProfileFinder testFinder = prefixes -> {
            ImmutableSet.Builder<String> profiles = ImmutableSet.builder();
            for (int profileCount = 0; profileCount < 40; profileCount++) {
                for (int implCount = 0; implCount < 3; implCount++) {
                    profiles.add("wonderland-profile-" + profileCount + ".impl-" + implCount);
                }
            }
            return profiles.build();
        };

        WonderlandContextSelector.create(Collections.emptySet(), Collections.singleton(testFinder))
                .showSelectionIfUnconfigured();
    }

}
