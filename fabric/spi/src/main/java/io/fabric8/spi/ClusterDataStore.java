package io.fabric8.spi;

import io.fabric8.api.ContainerIdentity;

/**
 * A cluster wide data store
 *
 * @author Thomas.Diesler@jboss.com
 * @since 18-Apr-2014
 */
public interface ClusterDataStore {

    ContainerIdentity createContainerIdentity(ContainerIdentity parentId, String prefix);
}