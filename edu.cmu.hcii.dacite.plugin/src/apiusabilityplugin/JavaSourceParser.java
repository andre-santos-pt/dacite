package apiusabilityplugin;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class JavaSourceParser {
	private final ASTParser parser;
	private CompilationUnit unit;

	public JavaSourceParser(String code, String className, String encoding) {
		parser = ASTParser.newParser(AST.JLS4);
//		parser.setResolveBindings(true);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);		
//		parser.setEnvironment(null, new String[0], new String[] {encoding}, true);
//		parser.setUnitName(className);
		parser.setSource(code.toCharArray());
	}
	

	public CompilationUnit getCompilationUnit() {
		if(unit == null) throw new IllegalStateException("Parse not executed yet");
		return unit;
	}

	public void parse(ASTVisitor visitor) {
		if(unit != null) throw new IllegalStateException("Parse already executed");
			
		unit = (CompilationUnit) parser.createAST(null);
		
//		if(unit.getProblems().length > 0)
//			throw new RuntimeException("code has compilation errors");
		
		unit.accept(visitor);
	}

	
}