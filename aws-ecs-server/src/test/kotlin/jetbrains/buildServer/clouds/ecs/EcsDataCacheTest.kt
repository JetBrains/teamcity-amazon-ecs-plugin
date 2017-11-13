package jetbrains.buildServer.clouds.ecs

import jetbrains.buildServer.BaseTestCase
import jetbrains.buildServer.clouds.InstanceStatus
import junit.framework.Assert
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

/**
 * Created by Evgeniy Koshkin (evgeniy.koshkin@jetbrains.com) on 24.10.17.
 */
@Test
class EcsDataCacheTest : BaseTestCase() {
    private lateinit var cache: EcsDataCache

    @BeforeMethod
    override fun setUp() {
        super.setUp()
        setInternalProperty(ECS_CACHE_EXPIRATION_TIMEOUT, 50.toString())
        cache = EcsDataCacheImpl()
    }

    @AfterMethod
    override fun tearDown() {
        super.tearDown()
    }

    @Test
    fun testGetInstanceStatusShouldCache() {
        var invocationCount = 0
        val resolver = {
            invocationCount++
            InstanceStatus.RUNNING
        }
        Assert.assertEquals(InstanceStatus.RUNNING, cache.getInstanceStatus("foo", resolver))
        Assert.assertEquals(InstanceStatus.RUNNING, cache.getInstanceStatus("foo", resolver))
        Assert.assertEquals(InstanceStatus.RUNNING, cache.getInstanceStatus("boo", resolver))
        Assert.assertEquals(2, invocationCount)
    }

    @Test
    fun testGetInstanceStatusShouldExpires() {
        var invocationCount = 0
        val resolver = {
            invocationCount++
            when(invocationCount){
                1 -> InstanceStatus.STARTING
                2 -> InstanceStatus.RUNNING
                3 -> InstanceStatus.STOPPING
                4 -> InstanceStatus.STOPPED
                else -> InstanceStatus.UNKNOWN
            }
        }
        Assert.assertEquals(InstanceStatus.STARTING, cache.getInstanceStatus("foo", resolver))
        Assert.assertEquals(InstanceStatus.STARTING, cache.getInstanceStatus("foo", resolver))
        Assert.assertEquals(InstanceStatus.STARTING, cache.getInstanceStatus("foo", resolver))
        Thread.sleep(100)
        Assert.assertEquals(InstanceStatus.RUNNING, cache.getInstanceStatus("foo", resolver))
        Assert.assertEquals(InstanceStatus.RUNNING, cache.getInstanceStatus("foo", resolver))
        Thread.sleep(100)
        Assert.assertEquals(InstanceStatus.STOPPING, cache.getInstanceStatus("foo", resolver))
        Thread.sleep(100)
        Assert.assertEquals(InstanceStatus.STOPPED, cache.getInstanceStatus("foo", resolver))
    }
}