package edu.cmu.hcii.dacite.plugin.creation;

import org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import edu.cmu.hcii.dacite.plugin.Activator;
import edu.cmu.hcii.dacite.plugin.IApiUsabilityProposal;
import edu.cmu.hcii.dacite.plugin.Instruction;

public class FactoryVarProposal implements IApiUsabilityProposal {

	private ContentAssistInvocationContext context;
	private String lineContent;
	private String var;
	private Instruction instruction;
	private String documentation;
	private String displayString;
	
	private static Image icon = Activator.getImage("public_co.gif");
	
	private Point selection;
	
	public FactoryVarProposal(ContentAssistInvocationContext context, String lineContent, String var, Instruction instruction, String documentation) {
		this.context = context;
		this.lineContent = lineContent;
		this.var = var;
		this.instruction = instruction;
		this.documentation = documentation;
		displayString = instruction.getInstruction(var, true) + " : " + instruction.getReturnType(); // + " - " + instruction.getSimpleType();
	}

	@Override
	public void apply(IDocument document) {
		try {
			int offset = context.getInvocationOffset();
			String text = instruction.getInstruction(var, false);
			int del = instruction.charsToErase(lineContent);
			document.replace(offset, 0, text);
			selection = new Point(offset+text.length()-del, 0);
		} 
		catch (BadLocationException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Point getSelection(IDocument document) {
		return selection;
	}

	@Override
	public String getAdditionalProposalInfo() {

		return "<b>" + instruction.getDescription() + "</b><br><br>" + documentation;
	}

	@Override
	public String getDisplayString() {
		return displayString;
	}

	@Override
	public Image getImage() {
		return icon;
	}

	@Override
	public IContextInformation getContextInformation() {
		return null;
	}

	@Override
	public boolean isOnAssignment() {
		return true;
	}

}
