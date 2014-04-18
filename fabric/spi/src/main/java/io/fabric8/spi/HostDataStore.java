package io.fabric8.spi;

import io.fabric8.api.ContainerIdentity;

/**
 * A host wide data store
 *
 * @author thomas.diesler@jboss.com
 * @since 18-Apr-2014
 */
public interface HostDataStore {

    ContainerIdentity createManagedContainerIdentity(String prefix);

}