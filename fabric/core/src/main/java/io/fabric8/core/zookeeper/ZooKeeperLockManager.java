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

package io.fabric8.core.zookeeper;

import io.fabric8.api.FabricException;
import io.fabric8.api.Lock;
import io.fabric8.api.LockManager;
import io.fabric8.api.ReadWriteLock;
import io.fabric8.spi.scr.AbstractComponent;
import io.fabric8.spi.scr.ValidatingReference;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.framework.recipes.locks.InterProcessReadWriteLock;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;

import java.util.concurrent.TimeUnit;

@Component(policy = ConfigurationPolicy.IGNORE, immediate = true)
@Service(LockManager.class)
public class ZooKeeperLockManager extends AbstractComponent implements LockManager {

    @Reference(referenceInterface = CuratorFramework.class)
    private ValidatingReference<CuratorFramework> curator = new ValidatingReference<>();

    @Activate
    void activate() {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
      deactivateComponent();
    }

    @Override
    public ReadWriteLock readWriteLock(String path) {
        assertValid();
        try {
            curator.get().create().creatingParentsIfNeeded().forPath(path);
        } catch (Exception e) {
            FabricException.launderThrowable(e);
        }
        return new InternalReadWriteLock(new InterProcessReadWriteLock(curator.get(), path));
    }

    void bindCurator(CuratorFramework service) {
        this.curator.bind(service);
    }

    void unbindCurator(CuratorFramework service) {
        this.curator.unbind(service);
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
