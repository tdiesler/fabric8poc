/*
 * Copyright (C) 2010 - 2014 JBoss by Red Hat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 */

package io.fabric8.core.utils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlaceholderUtils {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([a-zA-Z0-9\\.\\-]+)}");
    private static final String BOX_FORMAT = "\\$\\{%s\\}";

    public static String substitute(String str, Map<String, String> properties) {
       return substitute(str, properties, new HashSet<String>());
    }

    private static String substitute(String str, Map<String, String> properties, Set<String> visited) {
        String result = str;
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(str);
        CopyOnWriteArraySet<String> copyOfVisited = new CopyOnWriteArraySet<>(visited);
        while (matcher.find()) {
            String name = matcher.group(1);
            String replacement = "";
            String toReplace = String.format(BOX_FORMAT, name);
            if (properties.containsKey(name) && !visited.contains(name)) {
                replacement = properties.get(name);
                replacement = replacement != null ? replacement : "";
                if (PLACEHOLDER_PATTERN.matcher(replacement).matches()) {
                    copyOfVisited.add(name);
                    replacement = substitute(replacement, properties, copyOfVisited);
                }
            }
            result = result.replaceAll(toReplace, replacement);
        }
        return result;
    }
}
