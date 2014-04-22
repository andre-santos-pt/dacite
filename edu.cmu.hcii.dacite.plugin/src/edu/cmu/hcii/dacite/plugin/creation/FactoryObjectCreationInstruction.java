package edu.cmu.hcii.dacite.plugin.creation;

import edu.cmu.hcii.dacite.plugin.IMethodInfo;
import edu.cmu.hcii.dacite.plugin.Instruction;

public class FactoryObjectCreationInstruction extends Instruction {

	public FactoryObjectCreationInstruction(IMethodInfo info) {
		super(info);
	}
	
	@Override
	public String getInstruction(String var, boolean preview) {
		String tmp = "f";
		return getSimpleType() + " " + tmp + " = ";
	}
	
	@Override
	public String getDescription() {
		return "Obtain factory object";
	}


	
	

}
