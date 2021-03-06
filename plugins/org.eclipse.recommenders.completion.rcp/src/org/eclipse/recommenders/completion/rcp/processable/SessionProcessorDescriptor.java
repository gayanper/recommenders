/**
 * Copyright (c) 2010, 2012 Darmstadt University of Technology.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marcel Bruch - initial API and implementation.
 */
package org.eclipse.recommenders.completion.rcp.processable;

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.base.Optional.fromNullable;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;

public class SessionProcessorDescriptor {

    private static final int DEFAULT_PRIORITY = 10;

    private final IConfigurationElement config;
    private final String id;
    private final String name;
    private final String description;
    private final int priority;
    private final boolean enabledByDefault;
    private final String preferencePage;
    private Image icon;

    private SessionProcessor processor;

    public SessionProcessorDescriptor(IConfigurationElement config) {
        this.config = config;
        this.id = config.getAttribute("id"); //$NON-NLS-1$
        this.name = config.getAttribute("name"); //$NON-NLS-1$
        this.description = firstNonNull(config.getAttribute("description"), ""); //$NON-NLS-1$ //$NON-NLS-2$
        String priorityString = config.getAttribute("priority"); //$NON-NLS-1$
        this.priority = priorityString == null ? DEFAULT_PRIORITY : Integer.parseInt(priorityString);
        String enabledByDefaultString = config.getAttribute("enabledByDefault"); //$NON-NLS-1$
        this.enabledByDefault = enabledByDefaultString == null ? true : Boolean.parseBoolean(enabledByDefaultString);
        this.preferencePage = config.getAttribute("preferencePage"); //$NON-NLS-1$
    }

    @VisibleForTesting
    public SessionProcessorDescriptor(String id, String name, String description, Image icon, int priority,
            boolean enabledByDefault, String preferencePage, SessionProcessor processor) {
        this.config = null; // Not needed as processor is created eagerly
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.priority = priority;
        this.enabledByDefault = enabledByDefault;
        this.preferencePage = preferencePage;
        this.processor = processor;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public synchronized Image getIcon() {
        if (icon == null) {
            // postpone expensive file operations until really necessary
            icon = createIcon(config);
        }
        return icon;
    }

    private static Image createIcon(IConfigurationElement config) {
        String pluginId = config.getContributor().getName();
        String iconPath = config.getAttribute("icon"); //$NON-NLS-1$
        return AbstractUIPlugin.imageDescriptorFromPlugin(pluginId, iconPath).createImage();
    }

    public int getPriority() {
        return priority;
    }

    public boolean isEnabledByDefault() {
        return enabledByDefault;
    }

    public synchronized SessionProcessor getProcessor() throws CoreException {
        if (processor == null) {
            processor = (SessionProcessor) config.createExecutableExtension("class"); //$NON-NLS-1$
            if (Boolean.getBoolean("org.eclipse.recommenders.completion.rcp.measure")) { //$NON-NLS-1$
                processor = new StopwatchSessionProcessor(processor);
            }
        }
        return processor;
    }

    public Optional<String> getPreferencePage() {
        return fromNullable(preferencePage);
    }

    @Override
    public String toString() {
        return getId();
    }
}
