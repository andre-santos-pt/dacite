package edu.cmu.hcii.dacite.plugin;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;

import edu.cmu.hcii.dacite.annotations.Factory;
import edu.cmu.hcii.dacite.annotations.FactoryMethod;
import edu.cmu.hcii.dacite.annotations.Helper;
import edu.cmu.hcii.dacite.annotations.Parent;
import edu.cmu.hcii.dacite.annotations.StaticFactory;
import edu.cmu.hcii.dacite.plugin.IMethodInfo.IParamInfo;

public class ApiAnnotationsProvider implements IApiInformationProvider {

	private File dir;

	// type -> method
	private Multimap<String, IMethodInfo> staticFactories;

	// type -> factory type
	private Multimap<String, String> factoryTypes;

	// (factory type, type) -> method
	private Table<String, String, Set<IMethodInfo>> factoryMethods;

	// type -> method
	private Multimap<String, IMethodInfo> helpers;
	private Multimap<String, IMethodInfo> decorators;

	private Multimap<String, IMethodInfo> builders;
	
	public ApiAnnotationsProvider(File dir) {
		this.dir = dir;
		staticFactories = ArrayListMultimap.create();
		factoryTypes = ArrayListMultimap.create();
		factoryMethods = HashBasedTable.create();
		
		helpers = ArrayListMultimap.create();
		decorators = ArrayListMultimap.create();
		builders = ArrayListMultimap.create();
	}
	
	

	@Override
	public void initialize() {
		for(File f : dir.listFiles()) {
			Scanner scanner;
			try {
				scanner = new Scanner(f);

				while(scanner.hasNextLine()) {
					String line = scanner.nextLine();

					String ownerType = f.getName();
					String[] parts = line.split(":");
					String annotation = parts[0];
					String foreignType = parts[1];

					if(annotation.equals(Factory.class.getSimpleName())) {
						for(String t : foreignType.split(","))
							factoryTypes.put(t, ownerType);
					}
					else if(annotation.equals(StaticFactory.class.getSimpleName())) {
						staticFactories.put(foreignType, new MethodInfo(ownerType, parts[2], params(parts[3]), parts[4], true));
					}
					else if(annotation.equals(FactoryMethod.class.getSimpleName())) {
						Set<IMethodInfo> set = factoryMethods.get(ownerType, foreignType);
						if(set == null) {
							set = new HashSet<IMethodInfo>();
							factoryMethods.put(ownerType, foreignType, set);
						}
						MethodInfo info = new MethodInfo(ownerType, parts[2], params(parts[3]), parts[4], false);
						set.add(info);
					}
					else if(annotation.equals(Builder.class.getSimpleName())) {
						IMethodInfo info = new MethodInfo(ownerType, parts[2], params(parts[3]), parts[4], true);
						builders.put(foreignType, info);
					}
					else if(annotation.equals(Helper.class.getSimpleName())) {
						boolean isStatic = parts[6].equals("static");
						IMethodInfo info = new MethodInfo(ownerType, parts[2], params(parts[3]), parts[4], isStatic, Integer.parseInt(parts[5]));
						helpers.put(foreignType, info);
					}
					else if(annotation.equals(Parent.class.getSimpleName())) {
						IMethodInfo info = new MethodInfo(ownerType, parts[2], params(parts[3]), parts[4], true, Integer.parseInt(parts[5]));
						decorators.put(foreignType, info);
					}
				}
				scanner.close();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	
	
	private IParamInfo[] params(String paramList) {
		if(paramList.trim().isEmpty())
			return new IParamInfo[0];
		else {
			String[] params = paramList.split(",");
			IParamInfo[] info = new IParamInfo[params.length];
			
			for(int i = 0; i < params.length; i++) {
				String[] parts = params[i].split("\\s+");
				info[i] = new MethodInfo.ParamInfo(parts[0], parts[1]);
			}
			return info; 
		}
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
			return Collections.emptySet();
	}


	@Override
	public Collection<IMethodInfo> getHelperMethods(String type) {
		return helpers.get(type);
	}

//	@Override
//	public Collection<IMethodInfo> getDecorators(String type) {
//		return decorators.get(type);
//	}

	@Override
	public Collection<IMethodInfo> getCompositeChildren(String type) {
		return decorators.get(type);
	}

	@Override
	public Collection<IMethodInfo> getBuilders(String type) {
		return builders.get(type);
	}



}
