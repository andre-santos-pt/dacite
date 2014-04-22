package creation;

import apiusabilityplugin.IMethodInfo;
import apiusabilityplugin.Instruction;

public class FactoryMethodInstruction extends Instruction {

	public FactoryMethodInstruction(IMethodInfo info) {
		super(info);
	}
	
	@Override
	public String getInstruction(String var, boolean preview) {
		StringBuffer s = new StringBuffer(var);
		s.append('.').append(getMethodName());
		appendParamsList(var, s, preview);
		return s.toString();
	}
	
	@Override
	public String getDescription() {
		return "Factory method";
	}


	
	@Override
	public int charsToErase(String line) {
		if(line.trim().endsWith("new")) {
			int i = line.length()-1;
			int j = 0;
			while(Character.isWhitespace(line.charAt(i))) {
				i--;
				j++;
			}
					
			return "new".length() + j;
		}
		return 0;
	}

	

}
