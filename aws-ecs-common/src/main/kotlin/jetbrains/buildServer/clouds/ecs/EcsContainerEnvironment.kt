/*
 * Copyright 2000-2020 JetBrains s.r.o.
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

/**
 * Created by ekoshkin (koshkinev@gmail.com) on 07.06.17.
 */

const val TEAMCITY_ECS_PREFIX = "TEAMCITY_ECS_"
const val TEAMCITY_ECS_PROVIDED_PREFIX = "TC_ECS_PROVIDED_"

const val SERVER_URL_ECS_ENV = TEAMCITY_ECS_PREFIX + "SERVER_URL"
const val SERVER_UUID_ECS_ENV = TEAMCITY_ECS_PREFIX + "SERVER_UUID"
const val IMAGE_ID_ECS_ENV = TEAMCITY_ECS_PREFIX + "IMAGE_ID"
const val PROFILE_ID_ECS_ENV = TEAMCITY_ECS_PREFIX + "CLOUD_PROFILE_ID"
const val INSTANCE_ID_ECS_ENV = TEAMCITY_ECS_PREFIX + "INSTANCE_ID"
const val AGENT_NAME_ECS_ENV = TEAMCITY_ECS_PREFIX + "AGENT_NAME"

const val OFFICIAL_IMAGE_SERVER_URL_ECS_ENV = "SERVER_URL"
const val ECS_CONTAINER_METADATA_URI = "ECS_CONTAINER_METADATA_URI"
const val ECS_CONTAINER_METADATA_FILE = "ECS_CONTAINER_METADATA_FILE"

const val REQUIRED_PROFILE_ID_CONFIG_PARAM = "system.cloud.profile_id"
