package operation;

import apiusabilityplugin.IMethodInfo;
import apiusabilityplugin.Instruction;


public class ObjectBuilderInstruction extends Instruction {

	private boolean includeVar;
	
	public ObjectBuilderInstruction(IMethodInfo info, boolean includeVar) {
		super(info);
		this.includeVar = includeVar;
	}

	@Override
	public String getInstruction(String var, boolean preview) {
		StringBuffer s = new StringBuffer();

		if(includeVar) {
			s.append(getSimpleType()).append(' ')
			.append(shortVarName(getSimpleType())).append(" = ");
		}
		
		s.append("new ").append(getSimpleType());
		appendParamsList(var, s, preview);
		s.append(".").append(getMethodName()).append("()");
		
		return s.toString();
	}

	@Override
	public String getDescription() {
		return "Object builder";
	}

	@Override
	public int charsToErase(String line) {

		int i = line.length()-1;
		int j = 0;
		while(i >= 0 && !Character.isWhitespace(line.charAt(i))) {
			i--;
			j++;
		}
		return j;
	}
}
