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

package org.jboss.renov8.resolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.renov8.Pack;
import org.jboss.renov8.PackLocation;
import org.jboss.renov8.PackVersion;
import org.jboss.renov8.Renov8Exception;
import org.jboss.renov8.config.DistConfig;
import org.jboss.renov8.config.PackConfig;

/**
 * Resolves all the distribution configuration and makes sure
 * there are no version conflicts among its dependencies.
 *
 * <p>It may also perform an update of all or specific pack producers
 * from the configuration (direct and/or transitive).
 *
 * <p>The versions of the packs are determined by navigating the pack
 * dependencies in a hierarchical way, i.e. the pack version which appears
 * to be closer to the top of the configuration overrides the versions that
 * appear to be lower on the same branch.
 *
 * <p>However, if different dependency branches reference different versions
 * of the same pack producer, the resolver gives up with an error. The version
 * conflict has to be resolved in the original configuration (most probably
 * wit a transitive pack config) by the user.
 *
 *
 * @author Alexey Loubyansky
 */
public class DistResolver<P extends Pack> {

    /**
     * Resolves the distribution configuration without checking for updates.
     *
     * @param packResolver  repository and application-specific pack resolver
     * @param config  distribution configuration
     * @return  an instance of the resolver
     * @throws Renov8Exception  in case of a failure
     */
    public static <P extends Pack> DistResolver<P> newInstance(PackResolver<P> packResolver, DistConfig config) throws Renov8Exception {
        return new DistResolver<P>(packResolver, config, null);
    }

    /**
     * Resolves the distribution configuration updating packs to the latest available versions.
     *
     * @param packResolver  repository and application-specific pack resolver
     * @param config  distribution configuration
     * @param updateProducers  list of producers that should be checked for updates.
     * In case the list is empty, all the producers (of direct distribution dependencies) will be checked for updates.
     * @return  an instance of the resolver
     * @throws Renov8Exception  in case of a failure
     */
    public static <P extends Pack> DistResolver<P> newInstance(PackResolver<P> packResolver, DistConfig config, String... updateProducers) throws Renov8Exception {
        return new DistResolver<P>(packResolver, config, updateProducers);
    }

    private final DistConfig originalConfig;
    private PackResolver<P> packResolver;
    private Map<String, ProducerRef<P>> producers = new HashMap<>();
    private Set<String> updateProducers = Collections.emptySet();
    private final List<ProducerRef<P>> visited = new ArrayList<>();
    private int updatedProducersTotal;
    private DistConfig updatedConfig;
    private List<P> ordered;

    protected DistResolver(PackResolver<P> packResolver, DistConfig config, String[] updateProducers) throws Renov8Exception {
        if(!config.hasPacks()) {
            throw new Renov8Exception("Config is empty");
        }
        this.originalConfig = config;
        this.packResolver = packResolver;
        if(updateProducers != null) {
            switch (updateProducers.length) {
                case 0:
                    final List<PackConfig> packs = config.getPacks();
                    this.updateProducers = new HashSet<>(packs.size());
                    for (int i = 0; i < packs.size(); ++i) {
                        this.updateProducers.add(packs.get(i).getLocation().getProducer());
                    }
                    break;
                case 1:
                    this.updateProducers = Collections.singleton(updateProducers[0]);
                    break;
                default:
                    this.updateProducers = new HashSet<>(updateProducers.length);
                    for (String producer : updateProducers) {
                        this.updateProducers.add(producer);
                    }
            }
        }
        resolveDeps(null, config.getPacks());
        if(!visited.isEmpty()) {
            throw new IllegalStateException("Unexpected resolver state: visited producer ref stack is not empty");
        }
    }

    /**
     * Whether the distribution configuration was updated during the resolution
     * @return  true in case the configuration was updated, otherwise - false
     */
    public boolean isConfigUpdated() {
        return updatedProducersTotal > 0;
    }

    /**
     * The actual configuration that was used by the resolver.
     * That will be the original configuration in case the configuration wasn't updated
     * or the updated configuration in case there were version updates.
     *
     * @return  actual configuration used by the resolver
     */
    public DistConfig getConfig() {
        if(updatedProducersTotal == 0) {
            return originalConfig;
        }
        if(updatedConfig != null) {
            return updatedConfig;
        }
        final DistConfig.Builder configBuilder = DistConfig.builder();
        int updatedProducersInConfig = 0;
        for (PackConfig packConfig : originalConfig.getPacks()) {
            final ProducerRef<P> pRef = this.producers.get(packConfig.getLocation().getProducer());
            if(packConfig.isTransitive()) {
                if(!pRef.isLoaded()) {
                    continue;
                }
                if(!pRef.isFlagOn(ProducerRef.UPDATED)) {
                    configBuilder.addPack(packConfig);
                    continue;
                }
                packConfig = PackConfig.forTransitive(pRef.location);
            } else if(!pRef.isFlagOn(ProducerRef.UPDATED)) {
                configBuilder.addPack(packConfig);
                continue;
            } else {
                packConfig = PackConfig.forLocation(pRef.location);
            }
            configBuilder.addPack(packConfig);
            pRef.setFlag(ProducerRef.VISITED);
            ++updatedProducersInConfig;
        }

        if(updatedProducersInConfig < updatedProducersTotal) {
            for(ProducerRef<P> pRef : producers.values()) {
                if(pRef.isFlagOn(ProducerRef.UPDATED) && !pRef.isFlagOn(ProducerRef.VISITED)) {
                    configBuilder.addPack(PackConfig.forTransitive(pRef.location));
                    pRef.setFlag(ProducerRef.VISITED);
                    if(++updatedProducersInConfig == updatedProducersTotal) {
                        break;
                    }
                }
            }
        }

        if(updatedProducersInConfig != updatedProducersTotal) {
            throw new IllegalStateException("Located " + updatedProducersInConfig
                    + " updated pack producers but had to end up with " + updatedProducersTotal);
        }
        for(ProducerRef<P> pRef : producers.values()) {
            if(pRef.isFlagOn(ProducerRef.VISITED)) {
                pRef.clearFlag(ProducerRef.VISITED);
                if(--updatedProducersInConfig == 0) {
                    break;
                }
            }
        }

        updatedConfig = configBuilder.build();
        return updatedConfig;
    }

    /**
     * Iterates through the packs that form the distribution (not in any specific order)
     * and invokes the handler for each one of them.
     *
     * @param handler  pack handler
     * @throws Renov8Exception  in case the handler fails to process a pack
     */
    public void handlePacks(PackHandler<P> handler) throws Renov8Exception {
        for(ProducerRef<P> pRef : producers.values()) {
            if(!pRef.isLoaded()) {
                continue;
            }
            handler.handle(pRef.getSpec());
        }
    }

    /**
     * List of distribution packs ordered respecting their dependencies and
     * the order in which they are listed in the distribution configuration.
     *
     * @return  list of distribution packs ordered respecting their dependencies and
     * the order in which they are listed in the distribution configuration
     * @throws Renov8Exception  in case the list couldn't be resolved
     */
    public List<P> getPacksInOrder() throws Renov8Exception {
        if(ordered != null) {
            return ordered;
        }
        ordered = new ArrayList<P>(producers.size());
        orderPacks(ordered, originalConfig.getPacks());
        return ordered;
    }

    private void orderPacks(List<P> list, List<PackConfig> packs) throws Renov8Exception {
        for(PackConfig pc : packs) {
            if(pc.isTransitive()) {
                continue;
            }
            final ProducerRef<P> pRef = producers.get(pc.getLocation().getProducer());
            if(pRef.isFlagOn(ProducerRef.ORDERED) || !pRef.setFlag(ProducerRef.VISITED)) {
                continue;
            }
            if(pRef.getSpec().hasDependencies()) {
                orderPacks(list, pRef.getSpec().getDependencies());
            }
            if(pRef.setFlag(ProducerRef.ORDERED)) {
                list.add(pRef.getSpec());
            }
            pRef.clearFlag(ProducerRef.VISITED);
        }
    }

    private void resolveDeps(ProducerRef<P> parent, List<PackConfig> depConfigs) throws Renov8Exception {
        final int visitedOffset = visited.size();
        int i = 0;
        while (i < depConfigs.size()) {
            final PackConfig pConfig = depConfigs.get(i);
            PackLocation pLoc = pConfig.getLocation();
            ProducerRef<P> depRef = producers.get(pLoc.getProducer());
            if(depRef == null) {
                int status = ProducerRef.VISITED;
                if(updateProducers.contains(pLoc.getProducer())) {
                    final PackVersion latestVersion = packResolver.getLatestVersion(pLoc);
                    if(!latestVersion.equals(pLoc.getVersion())) {
                        pLoc = new PackLocation(pLoc.getRepoId(), pLoc.getProducer(), pLoc.getChannel(), pLoc.getFrequency(), latestVersion);
                        status |= ProducerRef.UPDATED;
                        ++updatedProducersTotal;
                    }
                }

                depRef = new ProducerRef<P>(pLoc, status);
                if(parent != null || !pConfig.isTransitive()) {
                    depRef.setSpec(packResolver.resolve(pLoc));
                    visited.add(depRef);
                }
                producers.put(pLoc.getProducer(), depRef);
            } else if(depRef.isFlagOn(ProducerRef.VISITED)) {
                if(depRef.isLoaded()) {
                    parent.setDependency(i, depRef);
                } else {
                    // relevant root transitive dependency
                    depRef.setSpec(packResolver.resolve(depRef.location));
                    visited.add(depRef);
                }
            } else if(depRef.location.getVersion().equals(pLoc.getVersion())) {
                parent.setDependency(i, depRef);
            } else {
                throw new Renov8Exception(depRef.location.getProducer() + " version conflict: " + depRef.location.getVersion() + " vs " + pLoc.getVersion());
            }
            ++i;
        }
        if(visited.size() == visitedOffset) {
            return;
        }
        i = visitedOffset;
        int depIndex = -i;
        while (i < visited.size()) {
            final ProducerRef<P> depRef = visited.get(i);
            final List<PackConfig> deps = depRef.getSpec().getDependencies();
            if (!deps.isEmpty()) {
                resolveDeps(depRef, deps);
            }
            if (parent != null) {
                if(depIndex < 0) {
                    depIndex += i;
                }
                while(parent.isDependencySet(depIndex)) {
                    ++depIndex;
                }
                parent.setDependency(depIndex++, depRef);
            }
            ++i;
        }
        i = visited.size();
        while (i > visitedOffset) {
            visited.remove(--i).clearFlag(ProducerRef.VISITED);
        }
    }
}
