/*
 * Copyright 2009 the original author or authors.
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

package org.gradle.initialization;

import org.gradle.StartParameter;
import org.gradle.api.internal.GradleInternal;
import org.gradle.api.internal.SettingsInternal;
import org.gradle.api.internal.initialization.ClassLoaderScope;
import org.gradle.configuration.ScriptPlugin;
import org.gradle.configuration.ScriptPluginFactory;
import org.gradle.groovy.scripts.TextResourceScriptSource;
import org.gradle.internal.resource.TextResourceLoader;
import org.gradle.internal.time.Time;
import org.gradle.internal.time.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;


public class ScriptEvaluatingSettingsProcessor implements SettingsProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptEvaluatingSettingsProcessor.class);

    private final SettingsFactory settingsFactory;
    private final IGradlePropertiesLoader propertiesLoader;
    private final ScriptPluginFactory configurerFactory;
    private final TextResourceLoader textResourceLoader;

    public ScriptEvaluatingSettingsProcessor(ScriptPluginFactory configurerFactory,
                                             SettingsFactory settingsFactory,
                                             IGradlePropertiesLoader propertiesLoader, TextResourceLoader textResourceLoader) {
        this.configurerFactory = configurerFactory;
        this.settingsFactory = settingsFactory;
        this.propertiesLoader = propertiesLoader;
        this.textResourceLoader = textResourceLoader;
    }

    @Override
    public SettingsInternal process(GradleInternal gradle,
                                    SettingsLocation settingsLocation,
                                    ClassLoaderScope buildRootClassLoaderScope,
                                    StartParameter startParameter) {
        Timer settingsProcessingClock = Time.startTimer();
        Map<String, String> properties = propertiesLoader.mergeProperties(Collections.<String, String>emptyMap());
        TextResourceScriptSource settingsScript = new TextResourceScriptSource(textResourceLoader.loadFile("settings file", settingsLocation.getSettingsFile()));
        SettingsInternal settings = settingsFactory.createSettings(gradle, settingsLocation.getSettingsDir(), settingsScript, properties, startParameter, buildRootClassLoaderScope);
        gradle.getBuildListenerBroadcaster().beforeSettings(settings);
        applySettingsScript(settingsScript, settings);
        LOGGER.debug("Timing: Processing settings took: {}", settingsProcessingClock.getElapsed());
        return settings;
    }

    private void applySettingsScript(TextResourceScriptSource settingsScript, final SettingsInternal settings) {
        ClassLoaderScope settingsClassLoaderScope = settings.getClassLoaderScope();
        ScriptPlugin configurer = configurerFactory.create(settingsScript, settings.getBuildscript(), settingsClassLoaderScope, settings.getRootClassLoaderScope().deprecated(), true);
        configurer.apply(settings);
    }

}
