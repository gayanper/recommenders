package org.eclipse.recommenders.completion.rcp.it;

import static org.junit.Assert.assertEquals;

import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.internal.ui.text.java.ProposalInfo;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.recommenders.completion.rcp.processable.BaseRelevanceSessionProcessor;
import org.eclipse.recommenders.completion.rcp.processable.IProcessableProposal;
import org.eclipse.recommenders.completion.rcp.processable.IProposalTag;
import org.eclipse.recommenders.completion.rcp.processable.ProposalProcessorManager;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

@SuppressWarnings("restriction")
public class BaseRelevanceSessionProcessorTest {

    @Test
    public final void test() throws Exception {

        // setup
        IProcessableProposal p = new FakeProcessableProposal();
        p.setRelevance(200);
        p.setStyledDisplayString(new StyledString("some"));
        p.setProposalProcessorManager(new ProposalProcessorManager(p));
        BaseRelevanceSessionProcessor sut = new BaseRelevanceSessionProcessor();

        // exercise
        sut.process(p);
        p.getProposalProcessorManager().prefixChanged("");

        // verify
        assertEquals(200, p.getRelevance());
    }

    private static final class FakeProcessableProposal implements IProcessableProposal {
        private int relevance;
        private StyledString styledDisplayString;
        private ProposalProcessorManager mgr;
        private String prefix;

        @Override
        public Point getSelection(IDocument document) {
            return null;
        }

        @Override
        public Image getImage() {
            return null;
        }

        @Override
        public String getDisplayString() {
            return styledDisplayString.toString();
        }

        @Override
        public IContextInformation getContextInformation() {
            return null;
        }

        @Override
        public String getAdditionalProposalInfo() {
            return null;
        }

        @Override
        public void apply(IDocument document) {
        }

        @Override
        public int getRelevance() {
            return relevance;
        }

        @Override
        public void setStyledDisplayString(StyledString styledDisplayString) {
            this.styledDisplayString = styledDisplayString;

        }

        @Override
        public void setRelevance(int newRelevance) {
            relevance = newRelevance;
        }

        @Override
        public void setProposalProcessorManager(ProposalProcessorManager mgr) {
            this.mgr = mgr;

        }

        @Override
        public StyledString getStyledDisplayString() {
            return styledDisplayString;
        }

        @Override
        public ProposalProcessorManager getProposalProcessorManager() {
            return mgr;
        }

        @Override
        public String getPrefix() {
            return prefix;
        }

        @Override
        public Optional<CompletionProposal> getCoreProposal() {
            return null;
        }

        @Override
        public void setImage(Image image) {
        }

        @Override
        public void setTag(IProposalTag key, Object value) {
        }

        @Override
        public <T> Optional<T> getTag(IProposalTag key) {
            return null;
        }

        @Override
        public <T> T getTag(IProposalTag key, T defaultValue) {
            return null;
        }

        @Override
        public <T> Optional<T> getTag(String key) {
            return null;
        }

        @Override
        public <T> T getTag(String key, T defaultValue) {
            return null;
        }

        @Override
        public ImmutableSet<IProposalTag> tags() {
            return null;
        }

        @Override
        public void setProposalInfo(ProposalInfo info) {
        }
    }
}
