/*
 * #%L
 * Gravia :: Integration Tests :: Common
 * %%
 * Copyright (C) 2010 - 2014 JBoss by Red Hat
 * %%
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
 * limitations under the License.
 * #L%
 */
package io.fabric8.spi;

import io.fabric8.api.Identifiable;
import io.fabric8.api.ProfileIdentity;
import io.fabric8.api.ProfileItem;

import java.util.Set;


/**
 * The internal profile state
 *
 * @author Thomas.Diesler@jboss.com
 * @since 14-Mar-2014
 */
public interface ProfileState extends Identifiable<ProfileIdentity> {

    ProfileVersionState getProfileVersion();

    Set<ProfileState> getParents();

    <T extends ProfileItem> Set<T> getProfileItems(Class<T> type);
}
