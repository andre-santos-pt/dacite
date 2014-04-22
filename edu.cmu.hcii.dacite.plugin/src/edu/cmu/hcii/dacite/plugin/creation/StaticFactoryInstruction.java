package edu.cmu.hcii.dacite.plugin.creation;

import edu.cmu.hcii.dacite.plugin.IMethodInfo;
import edu.cmu.hcii.dacite.plugin.Instruction;
import edu.cmu.hcii.dacite.plugin.IMethodInfo.IParamInfo;

public class StaticFactoryInstruction extends Instruction {

	public StaticFactoryInstruction(IMethodInfo info) {
		super(info);
	}
	
	@Override
	public String getInstruction(String var, boolean preview) {
		StringBuffer s = new StringBuffer(getSimpleType());
		s.append('.').append(getMethodName());
		appendParamsList(var, s, preview);
		return s.toString();
	}
	
	@Override
	public String getDescription() {
		return "Static factory";
	}


	
	// TODO pull up?
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
