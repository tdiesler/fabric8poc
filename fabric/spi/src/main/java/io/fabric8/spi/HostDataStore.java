package io.fabric8.spi;

/**
 * A host wide data store
 *
 * @author thomas.diesler@jboss.com
 * @since 18-Apr-2014
 */
public interface HostDataStore {

    String createManagedContainerIdentity(String prefix);

}