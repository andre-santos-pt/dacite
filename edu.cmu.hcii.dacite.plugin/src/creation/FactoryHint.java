package creation;

import java.util.Set;

import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import apiusabilityplugin.Activator;
import apiusabilityplugin.ContextInfo;
import apiusabilityplugin.IApiUsabilityProposal;
import apiusabilityplugin.Instruction;

public class FactoryHint implements IApiUsabilityProposal {

	private JavaContentAssistInvocationContext context;
	private String type;
	private String factoryType;

	private String instruction;
	private ContextInfo info;
	private Point selection;

	private String documentation;
	
	private static Image icon = Activator.getImage("class_prev.gif");

	public FactoryHint(JavaContentAssistInvocationContext context, String type, String factoryType, ContextInfo info, String documentation) {
		this.context = context;
		this.type = type;
		this.factoryType = factoryType;
		this.info = info;
		this.documentation = documentation;
		
		handleInstruction();
	}

	private void handleInstruction() {
		String type = factoryType;
		if(type.indexOf('.') != -1)
			type = type.substring(type.lastIndexOf('.')+1);
			
		String var = Instruction.shortVarName(type);
		Set<String> vars = info.getAllVars();
		int i = 1;
		while(vars.contains(var))
			var += i;

		instruction = type + " " + var + " = ";
	}

	@Override
	public void apply(IDocument document) {
		try {
			document.replace(context.getInvocationOffset(), 0, "null; // TODO");
			int line = document.getLineOfOffset(context.getInvocationOffset());
			int offset = document.getLineOffset(line);
			if(!info.containsImport(factoryType)) {
				int offsetImp = document.getLineOffset(info.importLine());
				String importStatement = "import " + factoryType + ";\n";
				document.replace(offsetImp, 0, importStatement);
				offset += importStatement.length();
			}

			int s = offset;
			while(Character.isWhitespace(document.getChar(s)))
				s++;

			document.replace(s, 0, instruction + "\n" + document.get(offset, s-offset));
			selection = new Point(s + instruction.length(), 0);
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
		return "<b>Previous step</b> - obtain a factory object <b>" + factoryType + "</b> in order to create objects of type <b>" + type + "</b>.<br><br>" +
	"<b>" + factoryType + "</b><br>" + documentation;
	}

	@Override
	public String getDisplayString() {
		return instruction;
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
