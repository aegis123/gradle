/*
 * Copyright 2017 the original author or authors.
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

package org.gradle.internal.logging.console

import org.gradle.internal.logging.OutputSpecification
import org.gradle.internal.logging.events.BatchOutputEventListener
import org.gradle.internal.logging.events.EndOutputEvent
import org.gradle.internal.logging.events.OutputEvent
import org.gradle.internal.nativeintegration.console.ConsoleMetaData
import org.gradle.internal.time.TimeProvider

import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture

class BuildStatusRendererTest extends OutputSpecification {
    def listener = Mock(BatchOutputEventListener)
    def console = new ConsoleStub()
    def consoleMetaData = Mock(ConsoleMetaData)
    def timeProvider = Mock(TimeProvider)
    def future = Mock(ScheduledFuture)
    def executor = Mock(ScheduledExecutorService)
    long currentTimeMs
    def renderer = new BuildStatusRenderer(listener, console.statusBar, console, consoleMetaData, timeProvider, executor)

    def setup() {
        executor.scheduleAtFixedRate(_, _, _, _) >> future
        timeProvider.getCurrentTime() >> { currentTimeMs }
    }

    def "schedules render at fixed rate once an root progress event is started"() {
        def event = startPhase(1, "message")

        when:
        renderer.onOutput([event] as ArrayList<OutputEvent>)

        then:
        1 * executor.scheduleAtFixedRate(_, _, _, _)
    }

    def "forwards event list to listener"() {
        def event = event("message")

        when:
        renderer.onOutput([event] as ArrayList<OutputEvent>)

        then:
        1 * listener.onOutput([event] as ArrayList)
    }

    def "formats given message with an incrementing timer"() {
        def event1 = startPhase(1, '<--> 0% INITIALIZING')
        def event2 = event('2')

        when:
        renderer.onOutput([event1] as ArrayList<OutputEvent>)

        then:
        statusBar.display == "<--> 0% INITIALIZING [0s]"

        when:
        currentTimeMs += 1000
        renderer.onOutput([event2] as ArrayList<OutputEvent>)

        then:
        statusBar.display == "<--> 0% INITIALIZING [1s]"
    }

    def "correctly cancel the future once the end event is received"() {
        def startEvent = startPhase(1, 'message')
        def end = new EndOutputEvent()

        given:
        renderer.onOutput([startEvent] as ArrayList<OutputEvent>)

        when:
        renderer.onOutput([end] as ArrayList<OutputEvent>)

        then:
        1 * future.cancel(false)
    }

    def "hides timer between build phases"() {
        given:
        def event1 = startPhase(1, '<--> 0% INITIALIZING')

        when:
        renderer.onOutput([event1] as ArrayList<OutputEvent>)

        then:
        statusBar.display == '<--> 0% INITIALIZING [0s]'

        when:
        renderer.onOutput([complete(1, '<--> 0% WAITING')] as ArrayList<OutputEvent>)

        then:
        statusBar.display == '<--> 0% WAITING'
    }

    def startPhase(Long id, String description) {
        start(id: id, parentId: null, category: BuildStatusRenderer.BUILD_PROGRESS_CATEGORY, shortDescription: description)
    }

    private ConsoleStub.TestableRedrawableLabel getStatusBar() {
        console.statusBar as ConsoleStub.TestableRedrawableLabel
    }
}
