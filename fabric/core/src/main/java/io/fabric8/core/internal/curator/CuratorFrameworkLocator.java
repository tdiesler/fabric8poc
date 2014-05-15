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
package io.fabric8.core.internal.curator;

import io.fabric8.spi.scr.ValidatingReference;
import org.apache.curator.framework.CuratorFramework;

/**
 * Locate the {@link org.apache.curator.framework.CuratorFramework}
 *
 * @since 10-Dec-2013
 */
public final class CuratorFrameworkLocator {

    private static ValidatingReference<CuratorFramework> curator = new ValidatingReference<CuratorFramework>();

    // Hide ctor
    private CuratorFrameworkLocator() {
    }

    /**
     * Get the current CuratorFramework or null.
     */
    public static CuratorFramework getCuratorFramework() {
        return curator.getOptional();
    }

    public static void bindCurator(CuratorFramework curator) {
        CuratorFrameworkLocator.curator.bind(curator);
    }

    public static void unbindCurator(CuratorFramework curator) {
        CuratorFrameworkLocator.curator.unbind(curator);
    }
}
