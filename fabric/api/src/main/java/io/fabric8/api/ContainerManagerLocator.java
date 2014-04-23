package io.fabric8.api;

/**
 * The container manager locator
 *
 * @author thomas.diesler@jboss.com
 * @since 23-Apr-2014
 */
public final class ContainerManagerLocator {

    /**
     * Locate the container manager
     */
    public static ContainerManager getContainerManager() {
        return ServiceLocator.getRequiredService(ContainerManager.class);
    }

    // Hide ctor
    private ContainerManagerLocator() {
    }
}