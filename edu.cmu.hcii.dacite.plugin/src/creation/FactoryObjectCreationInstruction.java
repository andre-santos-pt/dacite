package creation;

import apiusabilityplugin.IMethodInfo;
import apiusabilityplugin.Instruction;

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
