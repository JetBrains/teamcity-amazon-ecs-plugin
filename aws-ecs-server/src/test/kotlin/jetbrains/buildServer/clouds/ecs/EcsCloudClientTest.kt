/*
 * Copyright 2000-2021 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.clouds.ecs

import jetbrains.buildServer.BaseTestCase
import jetbrains.buildServer.clouds.CloudClientParameters
import jetbrains.buildServer.clouds.CloudImage
import jetbrains.buildServer.clouds.CloudImageParameters
import jetbrains.buildServer.clouds.ecs.apiConnector.EcsApiConnector
import jetbrains.buildServer.util.TestFor
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
        return createClient(images, mapOf())
    }

    private fun createClient(images: List<EcsCloudImage>, params: Map<String, String>): EcsCloudClient {
        return createClient("server-uuid", "profile-id", images, MockCloudClientParameters(params))
    }

    private fun createClient(serverUuid: String, profileId: String, images: List<EcsCloudImage>, cloudClientParameters: CloudClientParameters): EcsCloudClient {
        return EcsCloudClient(images, updater, EcsCloudClientParametersImpl(cloudClientParameters), serverUuid, createTempDir(), profileId)
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
                allowing(image).canStartNewInstanceWithDetails(); will(returnValue(true))
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
                allowing(image).canStartNewInstanceWithDetails(); will(returnValue(true))
            }
        })
        val images = listOf(image)
        val paramsMap = mapOf(Pair(PROFILE_INSTANCE_LIMIT_PARAM, "1"))
        val cloudClient = createClient(images, paramsMap)
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
                allowing(image).canStartNewInstanceWithDetails(); will(returnValue(false))
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

    @Test
    @TestFor(issues= ["TW-62422"])
    fun testStartNewInstanceManyImages(){
        val imageCount = 4
        val imagesList = arrayListOf<EcsCloudImage>()
        for (i in 1..imageCount){
            imagesList.add(m.mock(EcsCloudImage::class.java, "image-$i"))
        }
        m.checking(object : Expectations() {
            init {
                var idx = 0
                for (image in imagesList) {
                    idx++;
                    allowing(image).name; will(Expectations.returnValue("image-$idx-name"))
                    allowing(image).id; will(Expectations.returnValue("image-$idx-id"))
                    allowing(image).runningInstanceCount; will(Expectations.returnValue(idx-1))
                    allowing(image).canStartNewInstanceWithDetails(); will(returnValue(idx%2==0))
                }
            }
        })
        val cloudClient = createClient(imagesList, mapOf(Pair(PROFILE_INSTANCE_LIMIT_PARAM, "8")))
        Assert.assertTrue(cloudClient.canStartNewInstance(imagesList[3]))
        Assert.assertFalse(cloudClient.canStartNewInstance(imagesList[2]))
        Assert.assertTrue(cloudClient.canStartNewInstance(imagesList[1]))
        Assert.assertFalse(cloudClient.canStartNewInstance(imagesList[0]))
    }
}

class MockCloudClientParameters(val params: Map<String, String>) : CloudClientParameters() {
    override fun getProfileId(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getParameter(name: String): String? {
        return params.get(name)
    }

    override fun getParameters(): MutableMap<String, String> {
        return params.toMutableMap()
    }

    override fun getProfileDescription(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getCloudImages(): MutableCollection<CloudImageParameters> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun listParameterNames(): MutableCollection<String> {
        return params.keys.toMutableList()
    }

}
