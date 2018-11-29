/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.renov8;

/**
 * Pack location is used to resolve the pack in the target repository
 * and check whether there are newer versions of the pack available in the repository.
 *
 * <li>Producer represents a specific artifact over the course of its evolution
 * (i.e. any version of the artifact)
 * <li>Channel represents a backward compatible stream of version updates
 * <li>Frequency represents the lowest acceptable quality/qualifier of the artifact version
 * acceptable as an update (frequency is optional and if not provided defaults to the Final
 * or GA release)
 * <li>Version is a specific release version of the pack
 *
 * Producer and version is what is actually necessary to resolve any specific pack.
 *
 * Channel and frequency are used only during updates to determine the next backward
 * compatible version of the pack.
 *
 * @author Alexey Loubyansky
 */
public class PackLocation {

    public static PackLocation create(String producer, PackVersion version) {
        return new PackLocation(null, producer, null, null, version);
    }

    private final String repoId;
    private final String producer;
    private final String channel;
    private final String frequency;
    private final PackVersion version;

    public PackLocation(String repoId, String producer, String channel, String frequency, PackVersion version) {
        this.repoId = repoId;
        this.producer = producer;
        this.channel = channel;
        this.frequency = frequency;
        this.version = version;
    }

    public String getRepoId() {
        return repoId;
    }

    public String getProducer() {
        return producer;
    }

    public String getChannel() {
        return channel;
    }

    public String getFrequency() {
        return frequency;
    }

    public PackVersion getVersion() {
        return version;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((channel == null) ? 0 : channel.hashCode());
        result = prime * result + ((frequency == null) ? 0 : frequency.hashCode());
        result = prime * result + ((producer == null) ? 0 : producer.hashCode());
        result = prime * result + ((repoId == null) ? 0 : repoId.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PackLocation other = (PackLocation) obj;
        if (channel == null) {
            if (other.channel != null)
                return false;
        } else if (!channel.equals(other.channel))
            return false;
        if (frequency == null) {
            if (other.frequency != null)
                return false;
        } else if (!frequency.equals(other.frequency))
            return false;
        if (producer == null) {
            if (other.producer != null)
                return false;
        } else if (!producer.equals(other.producer))
            return false;
        if (repoId == null) {
            if (other.repoId != null)
                return false;
        } else if (!repoId.equals(other.repoId))
            return false;
        if (version == null) {
            if (other.version != null)
                return false;
        } else if (!version.equals(other.version))
            return false;
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append(producer);
        if(repoId != null) {
            buf.append('@').append(repoId);
        }
        if(channel != null) {
            buf.append(':').append(channel);
            if(frequency != null) {
                buf.append('/').append(frequency);
            }
        }
        if(version != null) {
            buf.append('#').append(version);
        }
        return buf.toString();
    }
}
