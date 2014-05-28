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

package io.fabric8.core.zookeeper.locks;

import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.FabricException;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.framework.recipes.locks.InterProcessReadWriteLock;
import org.jboss.gravia.utils.IllegalArgumentAssertion;

import java.util.concurrent.TimeUnit;

public class ZooKeeperLockManager  {

    private static final String CONTAINER_IDENTITY_LOCK_PATH = "/fabric/locks/container/%s";

    private final CuratorFramework curator;

    public ZooKeeperLockManager(CuratorFramework curator) {
        this.curator = curator;
    }

    public ReadWriteLock readWriteLock(ContainerIdentity identity) {
        String path = getPathForContainerIdentity(identity);
        try {
            curator.create().creatingParentsIfNeeded().forPath(path);
        } catch (Exception e) {
            FabricException.launderThrowable(e);
        }
        return new InternalReadWriteLock(new InterProcessReadWriteLock(curator, path));
    }

    private String getPathForContainerIdentity(ContainerIdentity identity) {
        IllegalArgumentAssertion.assertNotNull(identity, "Container identity cannot be null");
        return String.format(CONTAINER_IDENTITY_LOCK_PATH, identity.toString());
    }

    private class InternalReadWriteLock implements ReadWriteLock {

        private final InterProcessReadWriteLock delegate;

        private InternalReadWriteLock(InterProcessReadWriteLock delegate) {
            this.delegate = delegate;
        }

        @Override
        public Lock readLock() {
            return new InternalLock(delegate.readLock());
        }

        @Override
        public Lock writeLock() {
            return new InternalLock(delegate.writeLock());
        }
    }

    private class InternalLock implements Lock {

        private final InterProcessMutex delegate;

        private InternalLock(InterProcessMutex mutex) {
            this.delegate = mutex;
        }

        @Override
        public void unlock() {
            try {
               delegate.release();
            } catch (Exception e) {
                FabricException.launderThrowable(e);
            }
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) {
            try {
                return delegate.acquire(time, unit);
            } catch (Exception e) {
                FabricException.launderThrowable(e);
                return false;
            }
        }
    }
}
