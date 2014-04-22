package edu.cmu.hcii.dacite.plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;

public interface IApiInformationProvider {

	void initialize();

	Collection<IMethodInfo> getStaticFactories(String type);
	
	Collection<String> getFactoryTypes(String type);

	Collection<IMethodInfo> getFactoryMethods(String factoryType, String type);

	
	Collection<IMethodInfo> getHelperMethods(String type);
	
//	Collection<IMethodInfo> getDecorators(String type);
	
	Collection<IMethodInfo> getCompositeChildren(String type);
	
	Collection<IMethodInfo> getBuilders(String type);
	
	public class Builder {

		private static class Provider implements IApiInformationProvider {

			private Multimap<String, IMethodInfo> staticFactories;
			private Multimap<String, String> factoryTypes;
			private Table<String, String, Set<IMethodInfo>> factoryMethods;		
			
			private Multimap<String, IMethodInfo> helpers;
			private Multimap<String, IMethodInfo> decorators;
			private Multimap<String, IMethodInfo> compositeChildren;
			
			private Multimap<String, IMethodInfo> builders;
			
			private Provider() {
				staticFactories = ArrayListMultimap.create();
				factoryTypes = ArrayListMultimap.create();
				factoryMethods = HashBasedTable.create();
				
				helpers = ArrayListMultimap.create();
				decorators = ArrayListMultimap.create();
				compositeChildren = ArrayListMultimap.create();
				
				builders = ArrayListMultimap.create();
			}
			
			
			@Override
			public void initialize() {

			}

			@Override
			public Collection<IMethodInfo> getStaticFactories(String type) {
				return staticFactories.get(type);
			}

			@Override
			public Collection<String> getFactoryTypes(String type) {
				return factoryTypes.get(type);
			}

			@Override
			public Collection<IMethodInfo> getFactoryMethods(String factoryType, String type) {
				if(factoryMethods.contains(factoryType, type))
					return factoryMethods.get(factoryType, type);
				else
					return Collections.emptyList();
			}


			@Override
			public Collection<IMethodInfo> getHelperMethods(String type) {
				return helpers.get(type);
			}
			
//			@Override
//			public Collection<IMethodInfo> getDecorators(String type) {
//				return decorators.get(type);
//			}


			@Override
			public Collection<IMethodInfo> getCompositeChildren(String type) {
				return compositeChildren.get(type);
			}


			@Override
			public Collection<IMethodInfo> getBuilders(String type) {
				return builders.get(type);
			}
		}

		
		
		
		
		
		private Provider provider;
		
		public Builder() {
			provider = new Provider();
		}
		
		public Builder staticFactory(Method m) {
			provider.staticFactories.put(m.getReturnType().getName(), new MethodInfo(m));
			return this;
		}

		
		public Builder staticFactory(String type, String methodName, String[] paramTypes) {
			try {
				Class<?> clazz = Class.forName(type);
				Method method = clazz.getMethod(methodName, params(paramTypes));
				return staticFactory(method);
			} 
			catch (Exception e) {
				System.err.println("error in static factory method: " + type + "." + methodName);
			} 
			return this;
		}
	
		
		public Builder factoryType(Class<?> f, Class<?> t) {
			provider.factoryTypes.put(t.getName(), f.getName());
			return this;
		}
		
		
		public Builder factoryType(String factoryType, String createType) {
			try {
				Class<?> fClazz = Class.forName(factoryType);
				Class<?> cClazz = Class.forName(createType);

				return factoryType(fClazz, cClazz);
			} 
			catch (Exception e) {
				System.err.println("error in factory type: " + factoryType);
			} 
			return this;
		}
		
		
		
		public Builder factoryMethod(Method m) {
			String ownerType = m.getDeclaringClass().getName();
			String foreignType = m.getReturnType().getName();
			
			Set<IMethodInfo> set = provider.factoryMethods.get(ownerType, foreignType);
			if(set == null) {
				set = new HashSet<IMethodInfo>();
				provider.factoryMethods.put(ownerType, foreignType, set);
			}
			
			set.add(new MethodInfo(m));	
			return this;
		}
		
		public Builder factoryMethod(String type, String methodName, String[] paramTypes) {
		
			try {
				Class<?> clazz = Class.forName(type);
				Class<?>[] params = params(paramTypes);
				Method method = clazz.getMethod(methodName, params);
				return factoryMethod(method);
			} 
			catch (Exception e) {
				System.err.println("error in factory method: " + type + "." + methodName);
			} 
			
			return this;
		}
	
		
		
		public Builder helper(Method m, int instanceParam) {
			provider.helpers.put(m.getParameterTypes()[instanceParam].getName(), new MethodInfo(m, instanceParam));
			return this;
		}
		
		public Builder helper(String type, String methodName, String[] paramTypes, int instanceParam) {
			try {
				Class<?> clazz = Class.forName(type);
				Class<?>[] params = params(paramTypes);
				Method method = clazz.getMethod(methodName, params);
				return helper(method, instanceParam);
			} 
			catch (Exception e) {
				System.err.println("error in helper method: " + type + "." + methodName);
			} 
			return this;
		}
		
		
		public Builder decorator(Constructor<?> c, int instanceParam) {
			provider.decorators.put(c.getParameterTypes()[instanceParam].getName(), new MethodInfo(c, instanceParam));
			return this;
		}
		
		
		public Builder decorator(String type, String[] paramTypes, int instanceParam) {
			try {
				Class<?> clazz = Class.forName(type);
				Class<?>[] params = params(paramTypes);
				Constructor<?> c = clazz.getConstructor(params);
				return decorator(c, instanceParam);
			} catch (Exception e) {
				System.err.println("error in decorator constructor: " + type);
			} 
			return this;
		}
		
		
		
		public Builder compositeChild(Constructor<?> c, int instanceParam) {
			provider.compositeChildren.put(c.getParameterTypes()[instanceParam].getName(), new MethodInfo(c, instanceParam));
			return this;
		}
		
		public Builder compositeChild(String type, String[] paramTypes, int instanceParam) {
			try {
				Class<?> clazz = Class.forName(type);
				Class<?>[] params = params(paramTypes);
				Constructor<?> c = clazz.getConstructor(params);
				return compositeChild(c, instanceParam);
			} catch (Exception e) {
				System.err.println("error in decorator constructor: " + type);
			} 
			return this;
		}

		
		
		public IApiInformationProvider create() {
			return provider;
		}
		
		
		private static Class<?>[] params(String[] paramTypes) throws ClassNotFoundException {
			Class<?>[] params = new Class<?>[paramTypes.length];
			
			for(int i = 0; i < params.length; i++) {
				params[i] = handlePrimitive(paramTypes[i]);
				
				if(params[i] == null)
					params[i] = Class.forName(paramTypes[i]);
			}
			return params;
		}
		
		private static Class<?> handlePrimitive(String type) {
			switch(type) {
			case "byte": return byte.class;
			case "short": return short.class;
			case "int": return int.class;
			case "long": return long.class;
			case "double": return double.class;
			case "float": return float.class;
			case "char": return char.class;
			case "boolean": return boolean.class;
			default: return null;
			}
		}
		
	}
	
}
