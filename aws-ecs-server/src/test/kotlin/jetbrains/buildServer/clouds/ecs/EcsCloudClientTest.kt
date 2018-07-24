package jetbrains.buildServer.clouds.ecs

import jetbrains.buildServer.BaseTestCase
import jetbrains.buildServer.clouds.CloudClientParameters
import jetbrains.buildServer.clouds.CloudImage
import jetbrains.buildServer.clouds.CloudImageParameters
import jetbrains.buildServer.clouds.InstanceStatus
import jetbrains.buildServer.clouds.ecs.apiConnector.EcsApiConnector
import org.jmock.Expectations
import org.jmock.Mockery
import org.junit.Assert
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

/**
 * Created by Evgeniy Koshkin (evgeniy.koshkin@jetbrains.com) on 29.09.17.
 */
@Test
class EcsCloudClientTest : BaseTestCase() {
    private lateinit var m:Mockery
    private lateinit var api:EcsApiConnector

    private val cache:EcsDataCache = object: EcsDataCache{
        override fun cleanInstanceStatus(arn: String) {
        }

        override fun getInstanceStatus(taskArn: String, resolver: () -> InstanceStatus): InstanceStatus {
            return resolver.invoke()
        }
    }

    private val updater:EcsInstancesUpdater = object: EcsInstancesUpdater{
        override fun registerClient(client: EcsCloudClient) {
        }

        override fun unregisterClient(client: EcsCloudClient) {
        }
    }

    @BeforeMethod
    @Throws(Exception::class)
    public override fun setUp() {
        super.setUp()
        m = Mockery()
        api = m.mock(EcsApiConnector::class.java)
    }

    @AfterMethod
    @Throws(Exception::class)
    public override fun tearDown() {
        m.assertIsSatisfied()
        super.tearDown()
    }

    private fun createClient(images: List<EcsCloudImage>): EcsCloudClient {
        return createClient(images, object: CloudClientParameters() {
            val map = hashMapOf<String, String>()

            override fun getParameter(name: String): String? {
                return map[name]
            }

            override fun getParameters(): MutableMap<String, String> {
                return map
            }

            override fun getProfileDescription(): String {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getCloudImages(): MutableCollection<CloudImageParameters> {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun listParameterNames(): MutableCollection<String> {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

        })
    }

    private fun createClient(images: List<EcsCloudImage>, cloudClientParameters: CloudClientParameters): EcsCloudClient {
        return createClient("server-uuid", "profile-id", images, cloudClientParameters)
    }

    private fun createClient(serverUuid: String, profileId: String, images: List<EcsCloudImage>, cloudClientParameters: CloudClientParameters): EcsCloudClient {
        return EcsCloudClient(images, updater, EcsCloudClientParametersImpl(cloudClientParameters), serverUuid, profileId)
    }

    @Test
    @Throws(Exception::class)
    fun testCanStartNewInstance_UnknownImage() {
        val cloudClient = createClient(emptyList())
        val image = m.mock(EcsCloudImage::class.java)
        m.checking(object : Expectations() {
            init {
                allowing<CloudImage>(image).id
                will(Expectations.returnValue("image-1-id"))
            }
        })
        Assert.assertFalse(cloudClient.canStartNewInstance(image))
    }

    @Test
    @Throws(Exception::class)
    fun testCanStartNewInstance() {
        val image = m.mock(EcsCloudImage::class.java)
        m.checking(object : Expectations() {
            init {
                allowing(image).name; will(returnValue("image-1-name"))
                allowing(image).id; will(returnValue("image-1-id"))
                allowing(image).runningInstanceCount; will(returnValue(0))
                allowing(image).canStartNewInstance(); will(returnValue(true))
            }
        })
        val images = listOf(image)
        val cloudClient = createClient(images)
        Assert.assertTrue(cloudClient.canStartNewInstance(image))
    }

    @Test
    @Throws(Exception::class)
    fun testCanStartNewInstance_ProfileLimit() {
        val image = m.mock(EcsCloudImage::class.java)
        m.checking(object : Expectations() {
            init {
                allowing(image).id; will(Expectations.returnValue("image-1-id"))
                allowing(image).runningInstanceCount; will(Expectations.returnValue(1))
                allowing(image).canStartNewInstance(); will(returnValue(true))
            }
        })
        val images = listOf(image)
        val cloudClientParameters = object : CloudClientParameters(){
            val map = hashMapOf(Pair(PROFILE_INSTANCE_LIMIT_PARAM, "1"))

            override fun getParameter(name: String): String? {
                return map[name]
            }

            override fun getParameters(): MutableMap<String, String> {
                return map
            }

            override fun getProfileDescription(): String {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getCloudImages(): MutableCollection<CloudImageParameters> {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun listParameterNames(): MutableCollection<String> {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

        }
        val cloudClient = createClient(images, cloudClientParameters)
        Assert.assertFalse(cloudClient.canStartNewInstance(image))
    }

    @Test
    @Throws(Exception::class)
    fun testCanStartNewInstance_ImageCanNotStartNewInstance() {
        val image = m.mock(EcsCloudImage::class.java)
        m.checking(object : Expectations() {
            init {
                allowing(image).name; will(Expectations.returnValue("image-1-name"))
                allowing(image).id; will(Expectations.returnValue("image-1-id"))
                allowing(image).runningInstanceCount; will(Expectations.returnValue(1))
                allowing(image).canStartNewInstance(); will(returnValue(false))
            }
        })
        val images = listOf(image)
        val cloudClient = createClient(images)
        Assert.assertFalse(cloudClient.canStartNewInstance(image))
    }

    @Test
    @Throws(Exception::class)
    fun testDuplicateImageName() {
        val image1 = m.mock(EcsCloudImage::class.java, "1")
        val image2 = m.mock(EcsCloudImage::class.java, "2")
        m.checking(object : Expectations() {
            init {
                allowing(image1).id; will(Expectations.returnValue("image-1-id"))
                allowing(image1).name; will(Expectations.returnValue("image"))
                allowing(image1).runningInstanceCount; will(Expectations.returnValue(0))
                allowing(image2).id; will(Expectations.returnValue("image-2-id"))
                allowing(image2).name; will(Expectations.returnValue("image"))
                allowing(image2).runningInstanceCount; will(Expectations.returnValue(0))
            }
        })
        createClient(listOf(image1, image2))
    }
}