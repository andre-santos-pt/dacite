package operation;

import java.util.Collection;

import apiusabilityplugin.IMethodInfo;
import apiusabilityplugin.Instruction;


public class StaticHelperInstruction extends Instruction {

	public StaticHelperInstruction(IMethodInfo info) {
		super(info);
	}
	
	@Override
	public String getInstruction(String var, boolean preview) {
		StringBuffer s = new StringBuffer();
		String retType = getReturnType();
		if(!retType.equals("void"))
			s.append(retType).append(' ').append(shortVarName(retType)).append(" = ");
		
		s.append(getSimpleType()).append('.').append(getMethodName());
		appendParamsList(var, s, preview);
		return  s.toString();
	}

	@Override
	public String getDescription() {
		return "Helper method";
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
