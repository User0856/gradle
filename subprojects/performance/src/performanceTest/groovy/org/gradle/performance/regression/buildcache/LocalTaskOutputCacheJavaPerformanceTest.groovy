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

package org.gradle.performance.regression.buildcache

import org.gradle.performance.fixture.BuildExperimentInvocationInfo
import org.gradle.performance.fixture.BuildExperimentListenerAdapter
import org.gradle.test.fixtures.file.TestFile
import spock.lang.Unroll

@Unroll
class LocalTaskOutputCacheJavaPerformanceTest extends AbstractTaskOutputCacheJavaPerformanceTest {

    private TestFile cacheDir

    def setup() {
        runner.addBuildExperimentListener(new BuildExperimentListenerAdapter() {
            @Override
            void beforeInvocation(BuildExperimentInvocationInfo invocationInfo) {
                if (cacheDir == null) {
                    cacheDir = temporaryFolder.file("local-cache")
                }
                if (isFirstRunWithCache(invocationInfo)) {
                    cacheDir.deleteDir().mkdirs()
                    def settingsFile = new TestFile(invocationInfo.getProjectDir()).file('settings.gradle')
                    settingsFile << """
                        if (GradleVersion.current() > GradleVersion.version('3.4')) {
                            buildCache {
                                local {
                                    directory = '${cacheDir.absoluteFile.toURI()}'
                                }
                            }
                        } else {    
                            System.setProperty('org.gradle.cache.tasks.directory', '${cacheDir.absolutePath}')
                        }
                    """.stripIndent()
                }
            }

        })
    }

    @Unroll
    def "clean #tasks on #testProject with local cache (parallel: #parallel)"() {
        def testId = "clean $tasks on $testProject with local cache"
        def tasksToRun = tasks.split(' ') as List
        if (parallel) {
            testId += " with parallel"
            tasksToRun.add("--parallel")
        }
        given:
        runner.testId = testId
        runner.testProject = testProject
        runner.gradleOpts = ["-Xms${testProject.daemonMemory}", "-Xmx${testProject.daemonMemory}"]
        runner.tasksToRun = tasksToRun

        when:
        def result = runner.run()

        then:
        result.assertCurrentVersionHasNotRegressed()

        where:
        [testProject, tasks] << scenarios
        parallel << [true, false]
    }

    def "clean #tasks on #testProject with empty local cache"() {
        given:
        runner.testProject = testProject
        runner.gradleOpts = ["-Xms${testProject.daemonMemory}", "-Xmx${testProject.daemonMemory}"]
        runner.tasksToRun = tasks.split(' ')
        runner.warmUpRuns = 6
        runner.runs = 8

        runner.addBuildExperimentListener(new BuildExperimentListenerAdapter() {
            @Override
            void beforeInvocation(BuildExperimentInvocationInfo invocationInfo) {
                if (isCleanupRun(invocationInfo)) {
                    cacheDir.deleteDir().mkdirs()
                }
            }
        })

        when:
        def result = runner.run()

        then:
        result.assertCurrentVersionHasNotRegressed()

        where:
        [testProject, tasks] << scenarios
    }
}
