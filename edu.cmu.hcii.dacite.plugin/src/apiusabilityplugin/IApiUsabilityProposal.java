package apiusabilityplugin;

import org.eclipse.jface.text.contentassist.ICompletionProposal;

public interface IApiUsabilityProposal extends ICompletionProposal {

	boolean isOnAssignment();
	
//	public interface OnAssignment extends IApiUsabilityProposal{ }
	
//	public interface OnObjectInvocation extends IApiUsabilityProposal { }
	
}
