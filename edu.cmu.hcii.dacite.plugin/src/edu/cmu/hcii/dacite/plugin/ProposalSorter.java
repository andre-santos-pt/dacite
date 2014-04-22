package edu.cmu.hcii.dacite.plugin;

import org.eclipse.jdt.ui.text.java.AbstractProposalSorter;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

public class ProposalSorter extends AbstractProposalSorter {

	@Override
	public int compare(ICompletionProposal p1, ICompletionProposal p2) {
		// pull up API usability proposals on assignment
		if(isAssignmentApiProposal(p1) && !isAssignmentApiProposal(p2))
			return -1;
		
		if(isAssignmentApiProposal(p2) && !isAssignmentApiProposal(p1))
			return 1;
		
		return getCompareString(p1).compareTo(getCompareString(p2));
	}

	private static boolean isAssignmentApiProposal(ICompletionProposal p) {
		return 
				p instanceof IApiUsabilityProposal &&
				((IApiUsabilityProposal) p).isOnAssignment();
	}
	
	private static String getCompareString(ICompletionProposal p) {
		return p instanceof ApiProposal ? ((ApiProposal) p).methodDisplayString() : p.getDisplayString();
	}
}
