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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ProjectProperties {
    private final static Properties p = new Properties();

    static {
        try(InputStream is = ProjectProperties.class.getResourceAsStream("/META-INF/project.properties")) {
            if (is != null) {
                p.load(is);
            }
        } catch (IOException e) {
            // ignore
        }
    }

    public static Map<String, String> get() {
        HashMap<String, String> props = new HashMap<>(p.size());
        p.entrySet().stream().forEach(e -> props.put("" + e.getKey(), "" + e.getValue()));
        return props;
    }
}
