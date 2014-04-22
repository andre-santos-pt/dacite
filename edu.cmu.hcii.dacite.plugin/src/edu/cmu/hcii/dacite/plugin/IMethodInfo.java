package edu.cmu.hcii.dacite.plugin;


public interface IMethodInfo {

	String getOwnerType();
	
	String getMethodName();
	
	IParamInfo[] getParams();
	
//	String[] getParamNames();
	
	String getReturnType();
	
	int getTargetIndex();

	boolean isStatic();
	
	public interface IParamInfo {
		String getType();
		String getSimpleType();
		String getName();
	}
}
