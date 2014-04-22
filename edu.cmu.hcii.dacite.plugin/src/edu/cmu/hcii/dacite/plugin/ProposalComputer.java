package edu.cmu.hcii.dacite.plugin;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.ui.JavadocContentAccess;
import org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import edu.cmu.hcii.dacite.plugin.IMethodInfo.IParamInfo;
import edu.cmu.hcii.dacite.plugin.creation.FactoryHint;
import edu.cmu.hcii.dacite.plugin.creation.FactoryVarProposal;

public class ProposalComputer implements IJavaCompletionProposalComputer, IContextInformation {

	private final String ID = "[a-zA-Z_$][a-zA-Z\\d_$]*";

	private final String Q_ID = ID + "(\\." + ID + ")*";

	//private final String TYPE_ARGS = "(<(" + Q_ID + ")>)?";  //(," + Q_ID + ")*

	//private final String NEW_REF = Q_ID + "\\s*" + TYPE_ARGS + "\\s+" + ID + "\\s*=\\s*(new\\s*)?";

	//private final String REF_ASSIGN = ID + "\\s*=\\s*";

	private final String OBJ_INVOCATION = ID + "\\." + "(" + ID + ")?";

	private final Image icon; 

	public ProposalComputer() {
		icon = Activator.getImage("public2.gif");
	}

	@Override
	public void sessionStarted() { }

	@Override
	public void sessionEnded() { }

	@Override
	public String getErrorMessage() {
		return null;
	}

	@Override
	public List<ICompletionProposal> computeCompletionProposals(ContentAssistInvocationContext context, IProgressMonitor monitor) {
		List<ICompletionProposal> proposals = new ArrayList<>();
		if (context instanceof JavaContentAssistInvocationContext) {

			JavaContentAssistInvocationContext javaContext = (JavaContentAssistInvocationContext) context;//cont.getCompilationUnit().getElementAt(0).
			IJavaProject javaProject = javaContext.getProject();
			IDocument doc = context.getDocument();
			IType expectedType = javaContext.getExpectedType();
			int lineNumber = getLineNumber(context);
			int offset = context.getInvocationOffset();
			String line = getLineContent(doc, lineNumber);

		
			StringBuffer originalCode = new StringBuffer(doc.get());
			
			StringBuffer code = removeInvocationLine(originalCode, context);
			JavaSourceParser parser = new JavaSourceParser(code.toString(), null, null);
			ContextInfo varInfo = new ContextInfo(lineNumber);
			parser.parse(varInfo);

			String lineTrimmed = line.trim();

			//			if(lineTrimmed.matches(NEW_REF)) {
//			if(expectedType != null && (lineTrimmed.endsWith("=") || lineTrimmed.endsWith("new"))) {
				if(expectedType != null && leftCharIsEquals(originalCode, line.length(), offset)) {

				handleStaticFactories(javaContext, proposals, javaProject, expectedType, line, varInfo);

				handleFactories(javaContext, proposals, javaProject, expectedType, line, lineNumber, varInfo);
			}

			else if(lineTrimmed.matches(OBJ_INVOCATION)) {

				String[] parts = lineTrimmed.split("\\.");
				String var = parts[0].trim();
				String typeName = varInfo.varType(var);
				String text = parts.length > 1 ? parts[1] : "";

				if(typeName != null) {
					handleHelpers(javaContext, proposals, javaProject, line, varInfo, var, typeName, text);
				}
			} 
		}
		return proposals;
	}





	private StringBuffer removeInvocationLine(StringBuffer originalCode, ContentAssistInvocationContext context) {
		StringBuffer code = new StringBuffer(originalCode);
		int offset = context.getInvocationOffset();
		IRegion r = null;
		try {
			r = context.getDocument().getLineInformationOfOffset(offset);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		code.replace(r.getOffset(), r.getOffset() + r.getLength(), "");
		return code;
	}

	private boolean leftCharIsEquals(StringBuffer code, int lineLength, int offset) {
		
		for(int i = offset-1; i > offset-lineLength; i--) {
			char c = code.charAt(i);
			if(c == '=')
				return true;
			else if(!Character.isWhitespace(c) && c != '=')
				return false;
		}
		return false;	
	}

	private void handleStaticFactories(
			JavaContentAssistInvocationContext context,
			List<ICompletionProposal> proposals, IJavaProject proj,
			IType expectedType, String line, ContextInfo varInfo) {

		Collection<IType> types = new ArrayList<>();
		try {
			types.add(expectedType);
			ITypeHierarchy h = expectedType.newTypeHierarchy(new NullProgressMonitor());
			for(IType s : h.getAllSubtypes(expectedType))
				types.add(s);
		}
		catch (JavaModelException e) {
			e.printStackTrace();
		}

		for(IType t : types) {
			String type = t.getFullyQualifiedName();
			for(Instruction i : Activator.getInstance().getStaticFactories(type)) {
				if(i.isStatic()) {
					String documentation = extractDocumentation(proj, i.getQualifiedType(), i, false);
					ApiProposal p = new ApiProposal(context, this, i, varInfo, null, line, documentation, true);
					proposals.add(p);
				}
			}
		}
	}


	private void handleFactories(JavaContentAssistInvocationContext context,
			List<ICompletionProposal> proposals, IJavaProject proj,
			IType expectedType, String line, int lineNumber, ContextInfo varInfo) {


		Collection<IType> types = new ArrayList<>();
		try {
			ITypeHierarchy h = expectedType.newSupertypeHierarchy(new NullProgressMonitor());
			for(IType s : h.getAllTypes())
				types.add(s);
		}
		catch (JavaModelException e) {
			e.printStackTrace();
		}

		for(IType t : types) {
			String type = t.getFullyQualifiedName();
			for(String factoryType : Activator.getInstance().getFactoryTypes(type)) {
				Set<String> compatibleVars = varInfo.getVarsOfType(factoryType);

				if(compatibleVars.isEmpty()) {
					String documentation = extractDocumentation(proj, factoryType, null, true);
					FactoryHint hint = new FactoryHint(context, t.getElementName(), factoryType, varInfo, documentation);							
					proposals.add(hint);
				}
				else {
					for(String var : compatibleVars) {			
						for(Instruction i : Activator.getInstance().getFactories(factoryType, type)) {
							String documentation = extractDocumentation(proj, i.getQualifiedType(), i, false);
							FactoryVarProposal varProp = new FactoryVarProposal(context, line, var, i, documentation);
							proposals.add(varProp);
						}
					}
				}
			}

			for(Instruction i : Activator.getInstance().getBuilders(type)) {
				String documentation = extractDocumentation(proj, i.getQualifiedType(), i, true);
				ApiProposal p = new ApiProposal(context, this, i, varInfo, "??", line, documentation, true);
				proposals.add(p);
			}
		}
	}


	private void handleHelpers(JavaContentAssistInvocationContext javaContext,
			List<ICompletionProposal> proposals, IJavaProject javaProject,
			String line, ContextInfo varInfo, String var, String typeName,
			String text) {

		IType type = null;
		IType[] superTypes= null;
		try {
			type = javaProject.findType(typeName);
			if(type == null)
				return;

			ITypeHierarchy h = type.newSupertypeHierarchy(null);
			superTypes = h.getAllTypes();
		} 
		catch (JavaModelException e) {
			e.printStackTrace();
		}

		for(IType t : superTypes) {
			for(Instruction i : Activator.getInstance().getHelpers(t.getFullyQualifiedName())) {
				if(i.isStatic() && i.methodStartsWith(text)) {
					String documentation = extractDocumentation(javaProject, i.getQualifiedType(), i, false);
					ApiProposal p = new ApiProposal(javaContext, this, i, varInfo, var, line, documentation, false);
					proposals.add(p);
				}
			}

			//			for(Instruction i : Activator.getInstance().getDecorators(t.getFullyQualifiedName())) {
			//				String documentation = extractDocumentation(javaProject, i.getQualifiedType(), i, true);
			//				ApiProposal p = new ApiProposal(javaContext, this, i, varInfo, var, line, documentation, false);
			//				proposals.add(p);
			//			}

			if(text.isEmpty()) {
				for(Instruction i : Activator.getInstance().getCompositeChildren(t.getFullyQualifiedName())) {
					String documentation = extractDocumentation(javaProject, i.getQualifiedType(), i, true);
					ApiProposal p = new ApiProposal(javaContext, this, i, varInfo, var, line, documentation, false);
					proposals.add(p);
				}
			}
		}
	}


	private String removeTypeArgs(String type) {
		if(type.indexOf('<') == -1)
			return type;
		else
			return type.substring(0, type.indexOf('<'));
	}

	private String[] transformParameters(String[] paramTypes) {
		String[] params = new String[paramTypes.length];
		for(int i = 0; i < params.length; i++)
			//			params[i] = Signature.createTypeSignature(paramTypes[i], true);
			params[i] = "Q" + paramTypes[i].substring(paramTypes[i].lastIndexOf('.')+1) +";";

		return params;
	}

	private IMethod findMethod(IType type, String name, IParamInfo[] paramInfos) {

		try {
			IMethod[] methods = type.getMethods();
			for(IMethod m : methods) {
				if(m.getElementName().equals(name)) {

					String[] mParams = Arrays.copyOf(m.getParameterTypes(), m.getParameterTypes().length);
					replacePrimivites(mParams);
					if(mParams.length == paramInfos.length) {
						boolean allok = true;
						for(int i = 0; i < mParams.length; i++) {
							String dec = Signature.getSimpleName(mParams[i]);
							if(!dec.contains(paramInfos[i].getSimpleType()))
								allok = false;
						}
						if(allok)
							return m;
					}
				}
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		return null;
	}

	private void replacePrimivites(String[] mParams) {

		for(int i = 0; i < mParams.length; i++) {
			if(mParams[i].equals("" + Signature.C_BYTE))
				mParams[i] = "byte";
			else if(mParams[i].equals("" + Signature.C_SHORT))
				mParams[i] = "short";
			else if(mParams[i].equals("" + Signature.C_INT))
				mParams[i] = "int";
			else if(mParams[i].equals("" + Signature.C_LONG))
				mParams[i] = "long";

			else if(mParams[i].equals("" + Signature.C_DOUBLE))
				mParams[i] = "double";
			else if(mParams[i].equals("" + Signature.C_FLOAT))
				mParams[i] = "float";
			else if(mParams[i].equals("" + Signature.C_CHAR))
				mParams[i] = "char";
			else if(mParams[i].equals("" + Signature.C_BOOLEAN))
				mParams[i] = "boolean";
		}

	}

	private String extractDocumentation(IJavaProject proj, String type, Instruction i, boolean clazz) {
		try {
			IMember member =  proj.findType(type);
			if(member == null)
				return "Could not load documentation";

			if(!clazz)
				member = findMethod((IType) member, i.getMethodName(), i.getParams());

			if(member == null)
				return "Method not found";

			Reader reader = JavadocContentAccess.getHTMLContentReader(member, true, true);
			if(reader == null)
				return "Could not load documentation";

			return getString(reader);
		} 
		catch (JavaModelException e) {
			e.printStackTrace();
		}
		return "";
	}





	private static String getString(Reader reader) {
		StringBuffer buf= new StringBuffer();
		char[] buffer= new char[1024];
		int count;
		try {
			while ((count= reader.read(buffer)) != -1)
				buf.append(buffer, 0, count);
		} catch (IOException e) {
			return null;
		}
		return buf.toString();
	}

	@Override
	public List<IContextInformation> computeContextInformation(
			ContentAssistInvocationContext context, IProgressMonitor monitor) {
		List<IContextInformation> list = new ArrayList<>();
		list.add(this);
		return list;
	}





	private int getLineNumber(ContentAssistInvocationContext context) {
		try {
			return context.getDocument().getLineOfOffset(context.getInvocationOffset());
		} catch (BadLocationException e) {
			e.printStackTrace();
			return 0;
		}
	}

	// trimmed
	private String getLineContent(IDocument document, int line) {
		String lineContent = null;
		try {
			int lineLength = document.getLineLength(line);
			lineContent = document.get(document.getLineOffset(line), lineLength-1);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		return lineContent;
	}



	@Override
	public String getContextDisplayString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Image getImage() {
		return icon;
	}

	@Override
	public String getInformationDisplayString() {
		// TODO Auto-generated method stub
		return null;
	}


}
