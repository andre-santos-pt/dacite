package apiusabilityplugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;


public class MethodInfo implements IMethodInfo {

	
	public static class ParamInfo implements IParamInfo {
		private final String type;
		private final String name;
		
		public ParamInfo(String type, String name) {
			this.type = type;
			this.name = name;
		}

		@Override
		public String getType() {
			return type;
		}

		@Override
		public String getSimpleType() {
			int i = type.lastIndexOf('.');
			return i != -1 ? type.substring(i+1) : type;
		}
		
		@Override
		public String getName() {
			return name;
		}
	}
	
	
	
	private String qualifiedType;
	private String name;
	private IParamInfo[] params;
	private String returnType;
	private int paramIndex;
	private boolean isStatic;
	
	public MethodInfo(String ownerType, String name, IParamInfo[] params, String returnType, boolean isStatic) {
		this(ownerType, name, params, returnType, true, -1);
	}
	
	public MethodInfo(String ownerType, String name, IParamInfo[] params, String returnType, boolean isStatic, int paramIndex) {
		this.qualifiedType = ownerType;
		this.name = name;
		this.params = params;
		this.returnType = returnType;
		this.paramIndex = paramIndex;
		this.isStatic = isStatic;
	}
	
	public MethodInfo(Method m) {
		this(m, -1);
	}

	public MethodInfo(Method m, int paramIndex) {
		this(m.getDeclaringClass().getName(), m.getName(), params(m.getParameterTypes()), m.getReturnType().getName(), Modifier.isStatic(m.getModifiers()), paramIndex);
	}
	
	public MethodInfo(Constructor<?> c, int paramIndex) {
		this(c.getDeclaringClass().getName(), "new", params(c.getParameterTypes()), c.getDeclaringClass().getName(), Modifier.isStatic(c.getModifiers()), paramIndex);
	}
	
	private static IParamInfo[] params(Class<?>[] paramTypes)  {
		IParamInfo[] params = new IParamInfo[paramTypes.length];
		
		for(int i = 0; i < params.length; i++) {
			String name = paramTypes[i].getName().toLowerCase();
			if(name.indexOf('.') != -1)
				name = name.substring(name.indexOf('.') + 1);
			
			params[i] = new ParamInfo(paramTypes[i].getName(), name);
		}
		return params;
	}
	@Override
	public String getOwnerType() {
		return qualifiedType;
	}
	
	@Override
	public String getMethodName() {
		return name;
	}


	@Override
	public IParamInfo[] getParams() {
		return params;
	}

	@Override
	public String getReturnType() {
		return returnType;
	}

	@Override
	public int getTargetIndex() {
		return paramIndex;
	}

	@Override
	public boolean isStatic() {
		return isStatic;
	}
	
	@Override
	public String toString() {
		return name + Arrays.toString(params);
	}
	

}
