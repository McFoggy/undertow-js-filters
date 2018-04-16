/**
 * Copyright (C) 2018 Matthieu Brouillard [http://oss.brouillard.fr/undertow-jsfilters] (matthieu@brouillard.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.brouillard.oss.undertow;

import java.util.function.Function;

public class PropertiesResolver implements Function<String, String> {
    @Override
    public String apply(String s) {
        if (s == null) {
            return null;
        }

        if (s.startsWith("env:")) {
            return System.getProperty(s.substring("env:".length()), s);
        } else if (s.startsWith("prj:")) {
            return ProjectProperties.get().getOrDefault(s.substring("prj:".length()), s);
        }

        return s;
    }
}
