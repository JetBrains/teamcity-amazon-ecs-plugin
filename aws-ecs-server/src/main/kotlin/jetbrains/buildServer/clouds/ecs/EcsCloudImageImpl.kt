package jetbrains.buildServer.clouds.ecs

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.clouds.CloudErrorInfo
import jetbrains.buildServer.clouds.CloudInstance
import jetbrains.buildServer.clouds.ecs.apiConnector.EcsApiConnector
import java.util.concurrent.ConcurrentHashMap

class EcsCloudImageImpl(private val imageData: EcsCloudImageData, private val apiConnector: EcsApiConnector) : EcsCloudImage {
    private val LOG = Logger.getInstance(EcsCloudImageImpl::class.java.getName())

    private val myIdToInstanceMap = ConcurrentHashMap<String, EcsCloudInstance>()
    private var myCurrentError: CloudErrorInfo? = null

    override val instanceLimit: Int
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override val instanceCount: Int
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override val taskDefinition: String
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val cluster: String?
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val taskGroup: String?
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override fun getAgentPoolId(): Int? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getName(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getId(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getInstances(): MutableCollection<out CloudInstance> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getErrorInfo(): CloudErrorInfo? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun findInstanceById(id: String): CloudInstance? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun populateInstances() {
        try {
            for (taskArn in apiConnector.listTasks(cluster)) {
                val task = apiConnector.describeTask(taskArn)
                if(task != null){
                    val cloudInstance = EcsCloudInstanceImpl(this, task, apiConnector)
                    myIdToInstanceMap.put(cloudInstance.getInstanceId(), cloudInstance)
                } else {
                    LOG.warn("Failed to describe ECS task with arn $taskArn")
                }
            }
            myCurrentError = null
        } catch (ex: Throwable) {
            myCurrentError = CloudErrorInfo("Failed populate instances", ex.message.toString(), ex)
            throw ex
        }

    }

    override fun addInstance(instance: EcsCloudInstance) {
        myIdToInstanceMap.put(instance.instanceId, instance)
    }

    override fun deleteInstance(instance: EcsCloudInstance) {
        myIdToInstanceMap.remove(instance.instanceId)
    }
}