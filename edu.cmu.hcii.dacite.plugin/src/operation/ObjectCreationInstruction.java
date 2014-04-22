package operation;

import apiusabilityplugin.IMethodInfo;
import apiusabilityplugin.Instruction;


public class ObjectCreationInstruction extends Instruction {

	private boolean includeVar;
	private String description;
	
	public ObjectCreationInstruction(IMethodInfo info, boolean includeVar, String description) {
		super(info);
		this.includeVar = includeVar;
		this.description = description;
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
		return s.toString();
	}

	@Override
	public String getDescription() {
		return description;
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
