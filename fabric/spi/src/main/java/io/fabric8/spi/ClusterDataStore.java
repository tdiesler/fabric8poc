package io.fabric8.spi;

/**
 * A cluster wide data store
 *
 * @author thomas.diesler@jboss.com
 * @since 18-Apr-2014
 */
public interface ClusterDataStore {

    String createContainerIdentity(String parentId, String prefix);
}