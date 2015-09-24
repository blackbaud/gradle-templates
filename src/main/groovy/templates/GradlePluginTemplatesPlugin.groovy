/*
 * Copyright (c) 2011,2012 Eric Berry <elberry@tellurianring.com>
 * Copyright (c) 2013 Christopher J. Stehno <chris@stehno.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package templates

import org.gradle.api.Project
import templates.tasks.gradle.CreateGradlePluginTask
import templates.tasks.gradle.InitGradlePluginTask

/**
 * Adds basic tasks for bootstrapping gradle plugin projects. Adds createGradlePlugin, exportPluginTemplates, and
 * initGradlePlugin tasks. Also applies the groovy-templates plugin.
 */
class GradlePluginTemplatesPlugin extends GroovyTemplatesPlugin {

	void apply(Project project) {
		// Check to make sure GroovyTemplatesPlugin isn't already added.
		if (!project.plugins.findPlugin(GroovyTemplatesPlugin)) {
			project.apply(plugin: GroovyTemplatesPlugin)
		}

		project.task 'createGradlePlugin', type: CreateGradlePluginTask

		project.task 'initGradlePlugin', type: InitGradlePluginTask
	}
}