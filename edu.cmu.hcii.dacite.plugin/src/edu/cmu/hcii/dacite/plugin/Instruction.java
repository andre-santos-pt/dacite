package edu.cmu.hcii.dacite.plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import edu.cmu.hcii.dacite.plugin.IMethodInfo.IParamInfo;


public abstract class Instruction {

	private IMethodInfo info;
	private Collection<String> imports;
	
	public Instruction(IMethodInfo info) {
		this.info = info;
		handleImports();
	}

	private void handleImports() {
		imports = new ArrayList<>();
		imports.add(getQualifiedType());
		if(isImportableType(info.getReturnType()))
			imports.add(info.getReturnType());
		
		for(IParamInfo paramInfo : info.getParams())
			if(isImportableType(paramInfo.getType()))
				imports.add(paramInfo.getType());
	}
	
	private static boolean isImportableType(String type) {
		return 
				!type.matches("void|byte|short|int|long|double|float|char|boolean") &&
				!type.startsWith("java.lang.");
			 
	}
	
	String getQualifiedType() {
		return info.getOwnerType();
	}
	
	public String getSimpleType() {
		String type = getQualifiedType();
		if(type.indexOf('.') != -1)
			type = type.substring(type.lastIndexOf('.') + 1);

		return type; 
	}
	
	public Collection<String> getImports() {
		return Collections.unmodifiableCollection(imports);
	}
	
	 public abstract String getInstruction(String var, boolean preview);
	
	 public abstract String getDescription();
	 
	 public int charsToErase(String line) {
		 return 0;
	 }
	 
	 String getId() {
		 return getQualifiedType() + "." + getMethodName();
	 }
	 
	 public String getMethodName() {
		 return info.getMethodName();
	 }
	 
	 public IParamInfo[] getParams() {
		 return info.getParams();
	 }
	 
	 public int getInstanceParameter() {
		 return info.getTargetIndex();
	 }
	 
	 public String getReturnType() {
		 String s = info.getReturnType();
		 if(s.indexOf('.') != -1)
			 s = s.substring(s.lastIndexOf('.') + 1);
		 
		 return s;
	 }
	 
	 boolean methodStartsWith(String text) {
		 return getMethodName().startsWith(text);
	 }

	public boolean isNoArguments() {
		return info.getParams().length == 0;
	}
	 
	public boolean isSingleArgument() {
		return info.getParams().length == 1;
	}
	
	public boolean isStatic() {
		return info.isStatic();
	}
	
	@Override
	public String toString() {
		return info.toString();
	}

	protected void appendParamsList(String var, StringBuffer s, boolean includeTypes) {
		IParamInfo[] params = getParams();
		s.append('(');
		for(int i = 0; i < params.length; i++) {
			if(i != 0)
				s.append(", ");
			
			if(i == getInstanceParameter())
				s.append(var);
			else {
				if(includeTypes)
					s.append(params[i].getSimpleType() + " ");
				
				s.append(params[i].getName());
			}
		}
		s.append(')');
	}
	
	public static String shortVarName(String typeName) {
		String var = "" + Character.toLowerCase(typeName.charAt(0));
		for(int i = 1; i < typeName.length(); i++)
			if(Character.isUpperCase(typeName.charAt(i)))
				var += Character.toLowerCase(typeName.charAt(i));
		
		return var;
	}
	
	
}
