/**
 * Copyright (c) 2010, 2012 Darmstadt University of Technology.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marcel Bruch - Initial API
 */
package org.eclipse.recommenders.completion.rcp.processable;

import static com.google.common.base.Optional.fromNullable;
import static org.eclipse.recommenders.completion.rcp.processable.ProposalTag.IS_VISIBLE;
import static org.eclipse.recommenders.completion.rcp.processable.Proposals.copyStyledString;
import static org.eclipse.recommenders.utils.Checks.ensureIsNotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.ui.text.java.AbstractJavaCompletionProposal;
import org.eclipse.jdt.internal.ui.text.java.OverrideCompletionProposal;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.recommenders.utils.MethodHandleUtils;
import org.eclipse.recommenders.utils.Reflections;
import org.eclipse.swt.graphics.Image;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;

@SuppressWarnings({ "restriction", "unchecked" })
public class ProcessableOverrideCompletionProposal extends OverrideCompletionProposal implements IProcessableProposal {

    private static final Field F_METHOD_NAME = Reflections
            .getDeclaredField(true, OverrideCompletionProposal.class, "fMethodName").orNull(); //$NON-NLS-1$
    private static final Field F_PARAM_TYPES = Reflections
            .getDeclaredField(true, OverrideCompletionProposal.class, "fParamTypes").orNull(); //$NON-NLS-1$
    private static final Field F_RELEVANCE = Reflections
            .getDeclaredField(true, AbstractJavaCompletionProposal.class, "fRelevance").orNull(); //$NON-NLS-1$

    public static ProcessableOverrideCompletionProposal toProcessableProposal(OverrideCompletionProposal proposal,
            CompletionProposal coreProposal, JavaContentAssistInvocationContext context) {
        try {
            IJavaProject jproject = context.getProject();
            ICompilationUnit cu = context.getCompilationUnit();
            String methodName = (String) F_METHOD_NAME.get(proposal);
            String[] paramTypes = (String[]) F_PARAM_TYPES.get(proposal);
            int start = proposal.getReplacementOffset();
            int length = proposal.getReplacementLength();
            StyledString displayName = proposal.getStyledDisplayString();
            String completionProposal = proposal.getReplacementString();
            ProcessableOverrideCompletionProposal processableProposal = new ProcessableOverrideCompletionProposal(
                    jproject, cu, methodName, paramTypes, start, length, displayName, completionProposal, coreProposal);

            // CompletionProposalCollector.createMethodDeclarationProposal calls setImage/setRelevance in a separate
            // step on the freshly created object. Mimic this behavior.
            int relevance = F_RELEVANCE.getInt(proposal);
            processableProposal.setImage(proposal.getImage());
            processableProposal.setRelevance(relevance);

            return processableProposal;
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw Throwables.propagate(e);
        }
    }

    private final Map<IProposalTag, Object> tags = new HashMap<>();
    private final CompletionProposal coreProposal;

    private ProposalProcessorManager mgr;
    private String lastPrefix;
    private String lastPrefixStyled;
    private StyledString initialDisplayString;
    private Image decoratedImage;

    private ProcessableOverrideCompletionProposal(IJavaProject jproject, ICompilationUnit cu, String methodName,
            String[] paramTypes, int start, int length, StyledString displayName, String completionProposal,
            CompletionProposal coreProposal) {
        super(jproject, cu, methodName, paramTypes, start, length, displayName, completionProposal);
        this.coreProposal = coreProposal;
    }

    @Override
    public Image getImage() {
        if (decoratedImage == null) {
            decoratedImage = mgr.decorateImage(super.getImage());
        }
        return decoratedImage;
    }

    @Override
    public StyledString getStyledDisplayString() {
        if (initialDisplayString == null) {
            initialDisplayString = super.getStyledDisplayString();
            StyledString copy = copyStyledString(initialDisplayString);
            StyledString decorated = mgr.decorateStyledDisplayString(copy);
            setStyledDisplayString(decorated);
        }
        if (lastPrefixStyled != lastPrefix) {
            lastPrefixStyled = lastPrefix;
            StyledString copy = copyStyledString(initialDisplayString);
            StyledString decorated = mgr.decorateStyledDisplayString(copy);
            setStyledDisplayString(decorated);
        }
        return super.getStyledDisplayString();
    }

    @Override
    public boolean isPrefix(final String prefix, final String completion) {
        lastPrefix = prefix;
        boolean res = mgr.prefixChanged(prefix) || super.isPrefix(prefix, completion);
        setTag(IS_VISIBLE, res);
        return res;
    }

    @Override
    public String getPrefix() {
        return lastPrefix;
    }

    @Override
    public Optional<CompletionProposal> getCoreProposal() {
        return fromNullable(coreProposal);
    }

    @Override
    public ProposalProcessorManager getProposalProcessorManager() {
        return mgr;
    }

    @Override
    public void setProposalProcessorManager(ProposalProcessorManager mgr) {
        this.mgr = mgr;
    }

    @Override
    public void setTag(IProposalTag key, Object value) {
        ensureIsNotNull(key);
        if (value == null) {
            tags.remove(key);
        } else {
            tags.put(key, value);
        }
    }

    @Override
    public <T> Optional<T> getTag(IProposalTag key) {
        return Optional.fromNullable((T) tags.get(key));
    }

    @Override
    public <T> Optional<T> getTag(String key) {
        return Proposals.getTag(this, key);
    }

    @Override
    public <T> T getTag(IProposalTag key, T defaultValue) {
        T res = (T) tags.get(key);
        return res != null ? res : defaultValue;
    }

    @Override
    public <T> T getTag(String key, T defaultValue) {
        return this.<T>getTag(key).or(defaultValue);
    }

    @Override
    public ImmutableSet<IProposalTag> tags() {
        return ImmutableSet.copyOf(tags.keySet());
    }

    // No @Override, as introduced in JDT 3.12 (Neon) only
    protected String getPatternToEmphasizeMatch(IDocument document, int offset) {
        if (getTag(ProposalTag.IS_HIGHLIGHTED, false) || GET_PATTERN_TO_EMPHASIZE_MATCH_SUPER_METHOD == null) {
            return null;
        } else {
            try {
                return (String) GET_PATTERN_TO_EMPHASIZE_MATCH_SUPER_METHOD.invokeExact(this, document, offset);
            } catch (Throwable e) {
                throw Throwables.propagate(e);
            }
        }
    }

    private static MethodHandle GET_PATTERN_TO_EMPHASIZE_MATCH_SUPER_METHOD = MethodHandleUtils.getSuperMethodHandle(
            MethodHandles.lookup(), "getPatternToEmphasizeMatch", String.class, IDocument.class, int.class).orNull();
}
