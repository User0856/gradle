/*
 * Copyright 2013 the original author or authors.
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
package org.gradle.api.internal.file.collections;

import org.gradle.api.internal.file.FileCollectionInternal;
import org.gradle.api.internal.tasks.AbstractTaskDependency;
import org.gradle.api.internal.tasks.TaskDependencyContainer;
import org.gradle.api.internal.tasks.TaskDependencyResolveContext;
import org.gradle.api.tasks.TaskDependency;

/**
 * A {@link DelegatingFileCollection} whose delegate is created lazily.
 */
public abstract class LazilyInitializedFileCollection extends DelegatingFileCollection implements TaskDependencyContainer {
    private FileCollectionInternal delegate;

    public abstract String getDisplayName();

    @Override
    public String toString() {
        return getDisplayName();
    }

    @Override
    public TaskDependency getBuildDependencies() {
        return new AbstractTaskDependency() {
            @Override
            public void resolve(TaskDependencyResolveContext context) {
                LazilyInitializedFileCollection.this.resolve(context);
            }
        };
    }

    public abstract FileCollectionInternal createDelegate();

    @Override
    public final synchronized FileCollectionInternal getDelegate() {
        if (delegate == null) {
            delegate = createDelegate();
        }
        return delegate;
    }
}
