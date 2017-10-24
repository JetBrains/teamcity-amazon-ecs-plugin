package jetbrains.buildServer.clouds.ecs

import jetbrains.buildServer.clouds.InstanceStatus
import jetbrains.buildServer.serverSide.TeamCityProperties
import java.util.concurrent.ConcurrentHashMap

val CACHE_EXPIRATION_TIMEOUT_PROPERTY = "teamcity.ecs.cache.expirationTimeout"

class EcsDataCacheImpl : EcsDataCache {
    private val cache: MutableMap<String, CacheEntry<InstanceStatus>> = ConcurrentHashMap()

    override fun getInstanceStatus(taskArn: String, resolver: () -> InstanceStatus): InstanceStatus {
        val now = System.currentTimeMillis()
        synchronized(cache){
            val cacheEntry = cache.get(taskArn)
            if(cacheEntry == null || (now - cacheEntry.timestamp) > TeamCityProperties.getInteger(CACHE_EXPIRATION_TIMEOUT_PROPERTY, 10 * 1000)){
                cache.put(taskArn, CacheEntry(now, resolver.invoke()))
            }
        }
        return cache.get(taskArn)!!.data
    }

    class CacheEntry<T>(val timestamp: Long, val data: T)
}