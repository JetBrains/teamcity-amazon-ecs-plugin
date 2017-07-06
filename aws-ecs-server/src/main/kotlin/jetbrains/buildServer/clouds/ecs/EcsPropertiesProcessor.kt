package jetbrains.buildServer.clouds.ecs

import jetbrains.buildServer.serverSide.InvalidProperty
import jetbrains.buildServer.serverSide.PropertiesProcessor
import java.util.*

class EcsPropertiesProcessor : PropertiesProcessor {
    override fun process(properties: MutableMap<String, String>?): MutableCollection<InvalidProperty> {
        return Collections.emptyList()
    }

}