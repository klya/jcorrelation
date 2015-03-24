package com.maxifier.sync.impl;

import com.google.gson.Gson;
import com.maxifier.sync.resource.ResourceContext;
import com.maxifier.sync.resource.SyncResource;

import javax.persistence.EntityManager;
import javax.persistence.criteria.*;
import java.util.*;

public class ResourceContextImpl implements ResourceContext {
    private final IdentityHashMap<SyncResource, ResourceRecord> fetched = new IdentityHashMap<SyncResource, ResourceRecord>();
    private final ArrayList<ResourceRecord> resources = new ArrayList<ResourceRecord>();
    private final EntityManager entityManager;
    private final Gson gson = new Gson();

    public ResourceContextImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void lockResources(Collection<SyncResource> resource) {
        // unsupported
    }

    @Override
    public void unlockResources(Collection<SyncResource> resource) {
        // unsupported
    }

    @Override
    public void successfullyChanged(SyncResource resource) {
        ResourceRecord record = new ResourceRecord(resource, gson);
        resources.add(record);
    }

    @Override
    public void successfullyChanged(SyncResource resource, Collection<? extends SyncResource> sources) {
        ResourceRecord record = new ResourceRecord(resource, gson);
        for (SyncResource source : sources) {
            ResourceRecord sourceRecord = fetched.get(source);
            if (sourceRecord == null) {
                throw new IllegalArgumentException("Unknown source resource");
            }
            record.sources.add(sourceRecord);
        }
        resources.add(record);
    }

    @Override
    public void broadcastChanges() {
        for (ResourceRecord resource : resources) {
            entityManager.persist(resource);
        }
        resources.clear();
    }

    @Override
    public Walker getHistoryWalker() {
        return new WalkerImpl();
    }

    @Override
    public EntityManager getEntityManager() {
        return entityManager;
    }

    public class WalkerImpl implements Walker {
        private final CriteriaBuilder builder;
        private final CriteriaQuery<ResourceRecord> query;
        private final Root<ResourceRecord> root;
        private final List<Predicate> predicates;

        public WalkerImpl() {
            this.builder = entityManager.getCriteriaBuilder();
            this.query = builder.createQuery(ResourceRecord.class);
            this.root = query.from(ResourceRecord.class);
            this.predicates = new ArrayList<Predicate>();
            query.orderBy(builder.desc(root.get("id")));
        }

        @Override
        public Walker stepOn(SyncResource resource) {
            String prefix = resource.getName().concat("/%");
            predicates.add(
                builder.like(root.<String>get("resourceName"), builder.literal(prefix))
            );
            return this;
        }

        @Override
        public Walker stepOn(SyncResource resource, SyncResource mark) {
            String resourcePrefix = resource.getName().concat("/%");
            String markPrefix = mark.getName().concat("/%");
            Subquery<Integer> subquery = query.subquery(Integer.class).select(builder.literal(1));
            SetJoin<ResourceRecord, ResourceRecord> derivative = subquery.correlate(root).joinSet("derivatives");
            subquery.where(
                builder.like(derivative.<String>get("resourceName"), builder.literal(markPrefix))
            );
            predicates.add(
                builder.and(
                    builder.like(root.<String>get("resourceName"), builder.literal(resourcePrefix)),
                    builder.exists(subquery).not()
                )
            );
            return this;
        }

        @Override
        public Iterator<SyncResource> iterator() {
            Predicate[] predicates = this.predicates.toArray(new Predicate[this.predicates.size()]);
            final Iterator<ResourceRecord> it = entityManager
                .createQuery(query.where(builder.or(predicates)))
                .getResultList()
                .iterator();
            return new Iterator<SyncResource>() {
                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public SyncResource next() {
                    ResourceRecord record = it.next();
                    try {
                        Class<? extends SyncResource> resourceClass = Class.forName(record.className).asSubclass(SyncResource.class);
                        SyncResource resource = gson.fromJson(record.data, resourceClass);
                        fetched.put(resource, record);
                        return resource;
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }
}
