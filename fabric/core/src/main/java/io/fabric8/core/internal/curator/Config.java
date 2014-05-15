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

import static io.fabric8.core.internal.zookeeper.Constants.DEFAULT_CONNECTION_TIMEOUT_MS;
import static io.fabric8.core.internal.zookeeper.Constants.DEFAULT_RETRY_INTERVAL;
import static io.fabric8.core.internal.zookeeper.Constants.DEFAULT_SESSION_TIMEOUT_MS;
import static io.fabric8.core.internal.zookeeper.Constants.MAX_RETRIES_LIMIT;

class Config {

    private String zookeeperPassword;
    private int zookeeperRetryMax = MAX_RETRIES_LIMIT;
    private int zookeeperRetryInterval = DEFAULT_RETRY_INTERVAL;
    private int zookeeperConnectionTimeOut = DEFAULT_CONNECTION_TIMEOUT_MS;
    private int zookeeperSessionTimeout = DEFAULT_SESSION_TIMEOUT_MS;
    private String zookeeperUrl;

    String getZookeeperPassword() {
        return zookeeperPassword;
    }

    int getZookeeperRetryMax() {
        return zookeeperRetryMax;
    }

    int getZookeeperRetryInterval() {
        return zookeeperRetryInterval;
    }

    int getZookeeperConnectionTimeOut() {
        return zookeeperConnectionTimeOut;
    }

    int getZookeeperSessionTimeout() {
        return zookeeperSessionTimeout;
    }

    String getZookeeperUrl() {
        return zookeeperUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Config that = (Config) o;

        if (zookeeperConnectionTimeOut != that.zookeeperConnectionTimeOut) return false;
        if (zookeeperRetryInterval != that.zookeeperRetryInterval) return false;
        if (zookeeperRetryMax != that.zookeeperRetryMax) return false;
        if (zookeeperSessionTimeout != that.zookeeperSessionTimeout) return false;
        if (zookeeperPassword != null ? !zookeeperPassword.equals(that.zookeeperPassword) : that.zookeeperPassword != null)
            return false;
        if (zookeeperUrl != null ? !zookeeperUrl.equals(that.zookeeperUrl) : that.zookeeperUrl != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = zookeeperPassword != null ? zookeeperPassword.hashCode() : 0;
        result = 31 * result + zookeeperRetryMax;
        result = 31 * result + zookeeperRetryInterval;
        result = 31 * result + zookeeperConnectionTimeOut;
        result = 31 * result + zookeeperSessionTimeout;
        result = 31 * result + (zookeeperUrl != null ? zookeeperUrl.hashCode() : 0);
        return result;
    }
}
