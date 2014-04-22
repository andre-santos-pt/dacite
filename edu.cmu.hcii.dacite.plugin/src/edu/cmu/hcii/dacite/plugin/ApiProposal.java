package edu.cmu.hcii.dacite.plugin;

import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import edu.cmu.hcii.dacite.plugin.manipulation.ObjectCreationInstruction;

public class ApiProposal implements IApiUsabilityProposal {

	private JavaContentAssistInvocationContext context;
	private IContextInformation information;
	private Instruction instruction;
	private ContextInfo info;
	private String var;
	private Point selection;
	private String displayString;
	private String lineContent;

	private String documentation;

	private boolean assignment;

	private static final Image iconPublic = Activator.getImage("public2.gif");

	private static final Image iconClass = Activator.getImage("class_obj.gif");


	public ApiProposal(JavaContentAssistInvocationContext context, IContextInformation information, 
			Instruction instruction, ContextInfo info, String var, String lineContent, String documentation, boolean assignment) {
		this.context = context;
		this.information = information;
		this.instruction = instruction;
		this.info = info;
		this.var = var;
		this.lineContent = lineContent.replaceFirst("\\s+", "");

		this.documentation = documentation;
		this.assignment = assignment;
		displayString = instruction.getInstruction(var, true) + " : " + instruction.getReturnType(); //+ " - " + instruction.getSimpleType();
	}

	@Override
	public String getAdditionalProposalInfo() {
		return "<b>" + instruction.getDescription() + "</b><br><br>" + documentation;
	}

	@Override
	public void apply(IDocument document) {
		try {
			int offset = context.getInvocationOffset();
			for(String imp : instruction.getImports()) {
				if(!info.containsImport(imp)) {
					int offsetImp = document.getLineOffset(info.importLine());
					String importStatement = "import " + imp + ";\n";
					document.replace(offsetImp, 0, importStatement);
					offset += importStatement.length();
				}
			}
			//			if(!info.containsImport(instruction.getQualifiedType())) {
			//				int offsetImp = document.getLineOffset(info.importLine());
			//				String importStatement = "import " + instruction.getQualifiedType() + ";\n";
			//				document.replace(offsetImp, 0, importStatement);
			//				offset += importStatement.length();
			//			}

			//			String before = instruction.addBefore();
			//
			//			if(!before.isEmpty()) {
			//
			//			}

			String text = instruction.getInstruction(var, false);
			int del = instruction.charsToErase(lineContent);
			document.replace(offset-del, del, text);	

			if(instruction.isNoArguments() || instruction.isSingleArgument()) {
				selection = new Point(offset+text.length()-del, 0);
				//			else if(instruction.getParamTypes().length == 1)
				//				selection = new Point(offset+text.indexOf('(')+1, text.indexOf(')')-text.indexOf('(')-1);
			}
			else {
				if(instruction.getInstanceParameter() == 0) {
					int firstComma = text.indexOf(',');
					int nextComma = text.indexOf(',', firstComma+1);
					if(nextComma == -1)
						nextComma = text.indexOf(')', firstComma+1);
					selection = new Point(offset-del+firstComma+2, nextComma-firstComma-2);
				}

				// TODO other cases
			}

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
	public Image getImage() {
		return instruction instanceof ObjectCreationInstruction ? iconClass : iconPublic;
	}

	@Override
	public String getDisplayString() {
		return displayString;
	}

	@Override
	public IContextInformation getContextInformation() {
		return information;
	}

	public String methodDisplayString() {
		int i = displayString.indexOf('.');
		if(i != -1)
			return displayString.substring(i+1);

		return displayString;

	}

	@Override
	public boolean isOnAssignment() {
		return assignment;
	}

}
