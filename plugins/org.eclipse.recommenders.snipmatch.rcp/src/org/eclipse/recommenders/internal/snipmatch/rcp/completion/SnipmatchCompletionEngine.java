/**
 * Copyright (c) 2013 Madhuranga Lakjeewa.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Madhuranga Lakjeewa - initial API and implementation.
 */
package org.eclipse.recommenders.internal.snipmatch.rcp.completion;

import static org.eclipse.jface.text.IDocument.DEFAULT_CONTENT_TYPE;
import static org.eclipse.recommenders.internal.snipmatch.rcp.Constants.PREF_SEARCH_BOX_BACKGROUND;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.internal.ui.text.template.contentassist.TemplateInformationControlCreator;
import org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.recommenders.internal.snipmatch.rcp.l10n.Messages;
import org.eclipse.recommenders.snipmatch.ISnippet;
import org.eclipse.recommenders.snipmatch.rcp.SnippetAppliedEvent;
import org.eclipse.recommenders.utils.Nullable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Caret;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.google.common.base.Throwables;
import com.google.common.eventbus.EventBus;

/**
 * Snipmatch snippet completion engine.
 * <p>
 * Makes use of JFace content assist infrastructure - but in a bit unusual way. The event handling is probably calling
 * for trouble later.
 */
@SuppressWarnings("restriction")
public class SnipmatchCompletionEngine<T extends ContentAssistInvocationContext> {

    private static enum AssistantControlState {
        KEEP_OPEN,
        ENABLE_HIDE
    }

    private static final int SEARCH_BOX_WIDTH = 273;

    private final T context;
    private final AbstractContentAssistProcessor<T> processor;
    private final String filename;
    private final EventBus bus;
    private final ColorRegistry colorRegistry;
    private final FontRegistry fontRegistry;
    private final ContentAssistant assistant;

    private Shell searchShell;
    private ICompletionProposal selectedProposal;
    private StyledText searchText;
    private AssistantControlState state;

    public SnipmatchCompletionEngine(T context, AbstractContentAssistProcessor<T> processor, @Nullable String filename,
            EventBus bus, ColorRegistry colorRegistry, FontRegistry fontRegistry) {
        this.context = context;
        this.processor = processor;
        this.filename = filename;
        this.bus = bus;
        this.colorRegistry = colorRegistry;
        this.fontRegistry = fontRegistry;
        assistant = newContentAssistant();

    }

    private ContentAssistant newContentAssistant() {
        ContentAssistant assistant = new ContentAssistant() {

            @Override
            public void hide() {
                if (isFocused(searchText) && state != AssistantControlState.ENABLE_HIDE) {
                    // Ignore
                } else {
                    super.hide();
                    selectedProposal = null;
                }
            }

            private boolean isFocused(Control control) {
                Control focusControl = PlatformUI.getWorkbench().getDisplay().getFocusControl();
                return control.equals(focusControl);
            }
        };
        assistant.addCompletionListener(new ICompletionListener() {

            @Override
            public void assistSessionEnded(ContentAssistEvent event) {
                selectedProposal = null;
                if (searchShell != null) {
                    searchShell.dispose();
                }
            }

            @Override
            public void selectionChanged(ICompletionProposal proposal, boolean smartToggle) {
                selectedProposal = proposal;
            }

            @Override
            public void assistSessionStarted(ContentAssistEvent event) {
            }
        });

        assistant.setShowEmptyList(true);
        assistant.enablePrefixCompletion(true);
        assistant.enableColoredLabels(true);
        assistant.setRepeatedInvocationMode(true);
        assistant.setStatusLineVisible(false);

        assistant.setContentAssistProcessor(processor, DEFAULT_CONTENT_TYPE);
        assistant.setInformationControlCreator(new TemplateInformationControlCreator(SWT.LEFT_TO_RIGHT));
        assistant.setSorter(new ProposalSorter());

        return assistant;
    }

    public void show() {
        processor.setContext(context);
        processor.setFilename(filename);
        assistant.install(context.getViewer());
        state = AssistantControlState.KEEP_OPEN;
        createSearchPopup();
    }

    private void execute(String commandId) {
        try {
            assistant.getHandler(commandId).execute(null);
        } catch (ExecutionException e) {
            Throwables.propagate(e);
        }
    }

    private void createSearchPopup() {
        Shell parentShell = context.getViewer().getTextWidget().getShell();

        searchShell = new Shell(parentShell, SWT.ON_TOP);
        searchShell.setLayout(new FillLayout());
        searchShell.addListener(SWT.Traverse, new Listener() {

            @Override
            public void handleEvent(Event e) {
                if (e.detail == SWT.TRAVERSE_ESCAPE) {
                    state = AssistantControlState.ENABLE_HIDE;
                    assistant.uninstall();
                }
            }
        });
        searchText = new StyledText(searchShell, SWT.SINGLE);
        searchText.setFont(fontRegistry.get("org.eclipse.recommenders.snipmatch.rcp.searchTextFont")); //$NON-NLS-1$
        searchText.setBackground(colorRegistry.get(PREF_SEARCH_BOX_BACKGROUND));
        searchText.addFocusListener(new FocusAdapter() {

            @Override
            public void focusLost(FocusEvent e) {
                if (!assistant.hasProposalPopupFocus()) {
                    state = AssistantControlState.ENABLE_HIDE;

                    PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            assistant.uninstall();
                        }
                    });
                }
            }
        });
        searchText.addVerifyKeyListener(new VerifyKeyListener() {

            @Override
            public void verifyKey(VerifyEvent e) {
                ICompletionProposal appliedProposal = selectedProposal;
                switch (e.character) {
                case SWT.CR:
                    e.doit = false;
                    if (appliedProposal instanceof SnippetProposal) {
                        SnippetProposal snippetProposal = (SnippetProposal) appliedProposal;
                        state = AssistantControlState.ENABLE_HIDE;
                        assistant.uninstall();
                        if (snippetProposal.isValidFor(context.getDocument(), context.getInvocationOffset())) {
                            snippetApplied(snippetProposal);
                        }
                        snippetProposal.apply(context.getViewer(), (char) 0, SWT.NONE, context.getInvocationOffset());

                        Point selection = snippetProposal.getSelection(context.getDocument());
                        if (selection != null) {
                            context.getViewer().setSelectedRange(selection.x, selection.y);
                            context.getViewer().revealRange(selection.x, selection.y);
                        }
                    } else {
                        state = AssistantControlState.ENABLE_HIDE;
                        assistant.uninstall();
                    }
                    return;
                case SWT.TAB:
                    e.doit = false;
                    return;
                }

                // there is no navigation to support if no proposal is selected:
                if (appliedProposal == null) {
                    return;
                }
                // but if there is, let's navigate...
                switch (e.keyCode) {
                case SWT.ARROW_UP:
                    execute(ContentAssistant.SELECT_PREVIOUS_PROPOSAL_COMMAND_ID);
                    if (selectedProposal instanceof RepositoryProposal) {
                        execute(ContentAssistant.SELECT_PREVIOUS_PROPOSAL_COMMAND_ID);
                    }

                    return;
                case SWT.ARROW_DOWN:
                    execute(ContentAssistant.SELECT_NEXT_PROPOSAL_COMMAND_ID);
                    if (selectedProposal instanceof RepositoryProposal) {
                        execute(ContentAssistant.SELECT_NEXT_PROPOSAL_COMMAND_ID);
                    }
                    return;
                }
            }
        });
        searchText.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                String query = searchText.getText().trim();
                processor.setTerms(query);
                assistant.setEmptyMessage(Messages.COMPLETION_ENGINE_NO_SNIPPETS_FOUND);
                assistant.showPossibleCompletions();
                assistant.showContextInformation();

                if (selectedProposal instanceof RepositoryProposal) {
                    execute(ContentAssistant.SELECT_NEXT_PROPOSAL_COMMAND_ID);
                }
            }
        });

        placeShell();

        searchShell.open();
        searchShell.setFocus();
    }

    private void placeShell() {
        // Pack the shell so that it is high enough for the text field.
        searchShell.pack();

        int searchBoxHeight = searchShell.getSize().y;
        StyledText editorText = context.getViewer().getTextWidget();
        Caret caret = editorText.getCaret();
        int lineHeight = caret.getSize().y;
        Point location = caret.getLocation();
        Point anchor = editorText.toDisplay(location.x, location.y + lineHeight - searchBoxHeight);

        searchShell.setLocation(anchor.x, anchor.y);
        searchShell.setSize(SEARCH_BOX_WIDTH, searchBoxHeight);
    }

    private void snippetApplied(SnippetProposal proposal) {
        ISnippet snippet = proposal.getSnippet();
        String repoUri = null;
        // TODO How to get the repo uri?
        bus.post(new SnippetAppliedEvent(snippet.getUuid(), repoUri));
    }
}
