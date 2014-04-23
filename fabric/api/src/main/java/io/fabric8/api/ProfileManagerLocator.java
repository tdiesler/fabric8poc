package io.fabric8.api;

/**
 * The profile manager locator
 *
 * @author thomas.diesler@jboss.com
 * @since 23-Apr-2014
 */
public final class ProfileManagerLocator {

    /**
     * Locate the profile manager
     */
    public static ProfileManager getProfileManager() {
        return ServiceLocator.getRequiredService(ProfileManager.class);
    }

    // Hide ctor
    private ProfileManagerLocator() {
    }
}