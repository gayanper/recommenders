/**
 * Copyright (c) 2010, 2013 Darmstadt University of Technology.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Olav Lenz - initial API and implementation
 */
package org.eclipse.recommenders.coordinates;

import com.google.common.collect.ImmutableSet;

public interface IDependencyListener {

    ImmutableSet<DependencyInfo> getDependenciesForProject(DependencyInfo project);

    ImmutableSet<DependencyInfo> getProjects();

    ImmutableSet<DependencyInfo> getDependencies();
}
