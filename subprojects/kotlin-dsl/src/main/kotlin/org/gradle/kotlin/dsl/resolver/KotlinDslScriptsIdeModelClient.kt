/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:JvmName("KotlinDslScriptsIdeModelClient")

package org.gradle.kotlin.dsl.resolver

import org.gradle.kotlin.dsl.tooling.models.KotlinDslScriptsIdeModel
import org.gradle.tooling.ProjectConnection
import java.io.File


const val kotlinDslScriptsIdeModelTargets = "org.gradle.kotlin.dsl.provider.scripts"


/**
 * Kotlin DSL IDE model request for a set of scripts.
 */
data class KotlinDslScriptsIdeModelRequest @JvmOverloads constructor(

    /**
     * The set of scripts for which an IDE model is requested.
     * Must not be empty.
     */
    val scripts: List<File>,

    /**
     * Environment variables for the Gradle process.
     * Defaults will be used if `null` or empty.
     */
    val environmentVariables: Map<String, String>? = null,

    /**
     * Java home for the Gradle process.
     * Defaults will be used if `null`.
     */
    val javaHome: File? = null,

    /**
     * JVM options for the Gradle process.
     * Defaults to an empty list.
     */
    val jvmOptions: List<String> = emptyList(),

    /**
     * Gradle options.
     * Defaults to an empty list.
     */
    val options: List<String> = emptyList(),

    /**
     * Sets the leniency of the IDE model builder.
     *
     * When set to `false` the model builder will fail on the first encountered problem.
     *
     * When set to `true` the model builder will make a best effort to collect problems,
     * answer a reasonable model with editor reports for each script.
     *
     * Defaults to `false`.
     *
     * @see org.gradle.kotlin.dsl.tooling.models.KotlinBuildScriptModel.editorReports
     */
    val lenient: Boolean = false,

    /**
     * Request correlation identifier.
     * For IDE/Gradle logs correlation.
     * Defaults to a time based identifier.
     */
    val correlationId: String = newCorrelationId()
)


/**
 * Fetches Kotlin DSL IDE model for a set of scripts.
 *
 * @receiver the TAPI [ProjectConnection]
 * @param request the model request parameters
 * @return the IDE model for all requested scripts
 */
fun ProjectConnection.fetchKotlinDslScriptsIdeModel(request: KotlinDslScriptsIdeModelRequest): KotlinDslScriptsIdeModel =
    newKotlinDslScriptsIdeModelBuilder(request.valid()).get()


private
fun KotlinDslScriptsIdeModelRequest.valid(): KotlinDslScriptsIdeModelRequest = apply {
    require(scripts.isNotEmpty()) { "At least one script must be requested" }
}


private
fun ProjectConnection.newKotlinDslScriptsIdeModelBuilder(request: KotlinDslScriptsIdeModelRequest) =
    model(KotlinDslScriptsIdeModel::class.java).apply {

        if (request.environmentVariables?.isNotEmpty() == true) {
            setEnvironmentVariables(request.environmentVariables)
        }
        if (request.javaHome != null) {
            setJavaHome(request.javaHome)
        }

        if (request.lenient) setJvmArguments(request.jvmOptions + modelSpecificJvmOptions)
        else setJvmArguments(request.jvmOptions)

        forTasks(kotlinBuildScriptModelTask)

        val arguments = request.options.toMutableList()
        arguments += "-P$kotlinBuildScriptModelCorrelationId=${request.correlationId}"
        if (request.scripts.isNotEmpty()) {
            arguments += "-P$kotlinDslScriptsIdeModelTargets=${request.scripts.joinToString(":") { it.canonicalPath }}"
        }
        withArguments(arguments)
    }
