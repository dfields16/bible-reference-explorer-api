package com.bible.reference.explorer.api.Components;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CacheEvictor {

    @Autowired
    private CacheManager cacheManager;

    @Scheduled(cron = "${cache.clearCron}")
    public void evictCache() {
		cacheManager.getCacheNames()
		            .stream()
		            .map(x->cacheManager.getCache(x))
						.forEach(cache -> {
							if(cache instanceof ConcurrentMapCache) {
								int size = ((ConcurrentMapCache)cache).getNativeCache().size();
								if(size > 0){
									log.info("Clearing cacheName={} size={}", cache.getName(), size);
								}
							} else {
								log.info("Clearing cacheName={}", cache.getName());
							}
		            	cache.clear();
		            });
    }
}