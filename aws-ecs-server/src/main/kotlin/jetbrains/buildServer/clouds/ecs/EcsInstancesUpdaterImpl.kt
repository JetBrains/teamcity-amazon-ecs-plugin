package jetbrains.buildServer.clouds.ecs

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.serverSide.TeamCityProperties
import jetbrains.buildServer.serverSide.executors.ExecutorServices
import java.util.concurrent.TimeUnit

/**
 * Created by Evgeniy Koshkin (evgeniy.koshkin@jetbrains.com) on 13.11.17.
 */
class EcsInstancesUpdaterImpl(executors: ExecutorServices) : EcsInstancesUpdater {
    private val LOG = Logger.getInstance(EcsInstancesUpdaterImpl::class.java.getName())

    private val registeredClients: MutableCollection<EcsCloudClient> = ArrayList()

    override fun registerClient(client: EcsCloudClient) {
        registeredClients.add(client)
    }

    override fun unregisterClient(client: EcsCloudClient) {
        registeredClients.remove(client)
    }

    init {
        val delay = TeamCityProperties.getLong(ECS_TASKS_MONITORING_PERIOD, 1)
        executors.normalExecutorService.scheduleWithFixedDelay({ populateInstances() }, delay, delay, TimeUnit.MINUTES)
    }

    private fun populateInstances() {
        val populateInstancesStartTime = System.currentTimeMillis()
        registeredClients.forEach { client -> client.images.forEach { image -> (image as EcsCloudImage).populateInstances() } }
        LOG.debug("Populate ECS instances task finished in " + TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - populateInstancesStartTime) + " seconds")
    }
}

