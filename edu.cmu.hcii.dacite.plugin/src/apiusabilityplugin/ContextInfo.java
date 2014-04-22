package apiusabilityplugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

public class ContextInfo extends ASTVisitor {

	private Map<String,String> imports = new HashMap<>();

	private Map<String,String> varTypes = new HashMap<>();

	private int line;
	private int nextImport;
	
	public ContextInfo(int line) {
		this.line = line;
	}

	@Override
	public boolean visit(PackageDeclaration node) {
		nextImport = getLineNumber(node);
		return super.visit(node);
	}
	
	
	@Override
	public boolean visit(ImportDeclaration node) {
		String type = node.getName().toString();
		imports.put(type.substring(type.lastIndexOf('.')+1), type);
		nextImport = getLineNumber(node);
		
		return super.visit(node);
	}

	public boolean containsImport(String qualifiedType) {
		return imports.values().contains(qualifiedType);
	}
	
	@Override
	public boolean visit(Block node) {
		
//		List<ASTNode> list = (List<ASTNode>) node.getProperty(Block.STATEMENTS_PROPERTY.getId());
//		
//		for(ASTNode c : list)
//			System.out.println("\t" + c);
		int start = getLineNumber(node);
		int end = getEndLineNumber(node);
		return line >= start && line <= end;
	}

	@Override
	public boolean visit(VariableDeclarationStatement node) {
		if(getLineNumber(node) > line)
			return false;
		
		VariableDeclarationFragment dec = (VariableDeclarationFragment) node.fragments().get(0);
		String name = dec.getName().getIdentifier();
		
		Type t = node.getType();
		if(t.isParameterizedType())
			t = ((ParameterizedType) t).getType();
			
		if(t.isQualifiedType()) {
			varTypes.put(name, ((QualifiedType)t).getName().getFullyQualifiedName());
		}
		else if(t.isSimpleType()) {
			String type = ((SimpleType) t).getName().getFullyQualifiedName();
			varTypes.put(name, expand(type));
		}
		return super.visit(node);
	}

	@Override
	public boolean visit(VariableDeclarationFragment node) {
//		Expression e = node.getInitializer();
//		ITypeBinding binding = e.resolveTypeBinding();
//		varTypes.put(node.getName().getIdentifier(), binding.getQualifiedName());
//		
//		System.out.println("var " + node.getName().getIdentifier() + " " + binding.getQualifiedName());
		return super.visit(node);
	}

	public String expand(String type) {
		if(imports.containsKey(type))
			return imports.get(type);

		return type;
	}

	public String varType(String var) {
		return varTypes.get(var);
	}
	
	public Set<String> getAllVars() {
		return varTypes.keySet();
	}

	public Set<String> getVarsOfType(String type) {
		Set<String> set = new HashSet<>();
		for(Entry<String, String> e : varTypes.entrySet())
			if(e.getValue().equals(type))
				set.add(e.getKey());
		return set;
	}
	
	public int importLine() {
		return nextImport;
	}
	
	private static int getLineNumber(ASTNode node) {
		return ((CompilationUnit) node.getRoot()).getLineNumber(node.getStartPosition());	
	}
	
	private static int getEndLineNumber(ASTNode node) {
		return ((CompilationUnit) node.getRoot()).getLineNumber(node.getStartPosition()+node.getLength());
	}

	



}
