package apiusabilityplugin;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import operation.ObjectBuilderInstruction;
import operation.ObjectCreationInstruction;
import operation.StaticHelperInstruction;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import creation.FactoryMethodInstruction;
import creation.StaticFactoryInstruction;

public class Activator implements BundleActivator {
	private static final String PLUGIN_ID = "edu.cmu.hcii.dacite.plugin";
	private static final String EXT_POINT_API = PLUGIN_ID + ".api";
	private static final String EXT_POINT_API_INTERNAL = "internal";
	private static final String EXT_POINT_API_EXTERNAL = "external";

	private Collection<IApiInformationProvider> providers;

	private static Activator instance;

	public Activator() {
		instance = this;
		providers = new ArrayList<>();
	}

	public static Activator getInstance() {
		return instance;
	}
	
	public static Image getImage(String fileName) {
		return AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, "images/" + fileName).createImage();
	}

	@Override
	public void start(BundleContext context) throws Exception {
		loadPlugins();

	}

	private void loadPlugins() {
		for(IConfigurationElement e : Platform.getExtensionRegistry().getConfigurationElementsFor(EXT_POINT_API)) {
			if(e.getName().equals(EXT_POINT_API_INTERNAL))
				handleAnnotationIndexes(e);
			else if(e.getName().equals(EXT_POINT_API_EXTERNAL))
				handleExternalAnnotations(e);
		}
	}

	private void handleAnnotationIndexes(IConfigurationElement e) {
		String bundleId = e.getContributor().getName();

		URL entry = Platform.getBundle(bundleId).getEntry(e.getAttribute("path")); 
		String path = null;
		try {
			path = FileLocator.toFileURL(entry).getPath();
		} catch (IOException e1) {
			e1.printStackTrace();
		} 
		
		IApiInformationProvider p = new ApiAnnotationsProvider(new File(path));
		p.initialize();
		providers.add(p);
	}

	private void handleExternalAnnotations(IConfigurationElement e) {
		IApiInformationProvider.Builder builder = new IApiInformationProvider.Builder();

		for(IConfigurationElement t : e.getChildren("type")) {
			String type = t.getAttribute("type");

			for(IConfigurationElement c : t.getChildren("staticFactory")) {
				builder.staticFactory(type, c.getAttribute("name"), params(c.getChildren("param")) );
			}

			for(IConfigurationElement c : t.getChildren("factoryType")) {
				for(IConfigurationElement fm : c.getChildren("factoryMethod")) {
					builder.factoryType(type, fm.getAttribute("returnType"));
					builder.factoryMethod(type, fm.getAttribute("name"), params(fm.getChildren("param")) );
				}
			}

			for(IConfigurationElement c : t.getChildren("helperMethod")) {
				int instanceParam = 0;

				try {
					instanceParam = Integer.parseInt(c.getAttribute("instanceParam"));
				}
				catch(Exception ex) {

				}
				builder.helper(type, c.getAttribute("name"), params(c.getChildren("param")), instanceParam);
			}

			for(IConfigurationElement c : t.getChildren("composition")) {
				int instanceParam = 0;

				try {
					instanceParam = Integer.parseInt(c.getAttribute("instanceParam"));
				}
				catch(Exception ex) {

				}
				builder.compositeChild(type, params(c.getChildren("param")), instanceParam);
			}

		}
		providers.add(builder.create());
	}


	private String[] params(IConfigurationElement[] params) {
		String[] paramTypes = new String[params.length];
		for(int i = 0; i < params.length; i++)
			paramTypes[i] = params[i].getAttribute("type");
		return paramTypes;
	}



	@Override
	public void stop(BundleContext context) throws Exception {
		// TODO Auto-generated method stub

	}

	public Collection<Instruction> getStaticFactories(String type) {
		Set<Instruction> set = new HashSet<>();
		for(IApiInformationProvider provider : providers) {
			for(IMethodInfo info  : provider.getStaticFactories(type)) {
				set.add(new StaticFactoryInstruction(info));
			}
		}
		return set;
	}

	public Collection<String> getFactoryTypes(String type) {
		Set<String> set = new HashSet<>();
		for(IApiInformationProvider provider : providers) {
			set.addAll(provider.getFactoryTypes(type));
		}
		return set;
	}



	public Collection<Instruction> getFactories(String factoryType, String type) {
		Set<Instruction> set = new HashSet<>();
		for(IApiInformationProvider provider : providers) {
			for(IMethodInfo info : provider.getFactoryMethods(factoryType, type)) {
				set.add(new FactoryMethodInstruction(info));
			}
		}
		return set;
	}


	public Collection<Instruction> getBuilders(String type) {
		Set<Instruction> set = new HashSet<>();
		for(IApiInformationProvider provider : providers) {
			for(IMethodInfo info  : provider.getBuilders(type)) {
				set.add(new ObjectBuilderInstruction(info, false));
			}
		}
		return set;
	}








	public Collection<Instruction> getHelpers(String type) {
		Set<Instruction> set = new HashSet<>();
		for(IApiInformationProvider provider : providers) {
			for(IMethodInfo info  : provider.getHelperMethods(type)) {
				set.add(new StaticHelperInstruction(info));
			}
		}
		return set;
	}


	//	public Collection<Instruction> getDecorators(String type) {
	//		Set<Instruction> set = new HashSet<>();
	//		for(IApiInformationProvider provider : providers) {
	//			for(IMethodInfo info  : provider.getDecorators(type)) {
	//				set.add(new DecoratorObjectInstruction(info));
	//			}
	//		}
	//		return set;
	//	}

	public Collection<Instruction> getCompositeChildren(String type) {
		Set<Instruction> set = new HashSet<>();
		for(IApiInformationProvider provider : providers) {
			for(IMethodInfo info  : provider.getCompositeChildren(type)) {
				set.add(new ObjectCreationInstruction(info, true, "Object composition"));
			}
		}
		return set;
	}





	//	public static Set<Class<?>> loadClasses(String jarPath) {
	//
	//		Set<Class<?>> set = new HashSet<>();
	//		try {
	//			JarInputStream jarFile = new JarInputStream(new FileInputStream(jarPath));
	//			URL[] urls = { new URL("jar:file:" + jarPath +"!/") };
	//			ClassLoader cl = URLClassLoader.newInstance(urls);
	//
	//			JarEntry jarEntry;
	//
	//			while ((jarEntry = jarFile.getNextJarEntry()) != null) {
	//				if (jarEntry.getName().endsWith(".class")) {
	//					String className = jarEntry.getName().replaceAll("/", "\\.");
	//					className = className.substring(0, className.length() - ".class".length());
	//					Class<?> c = cl.loadClass(className);
	//					set.add(c);
	//				}
	//			}
	//			jarFile.close();
	//
	//		} 
	//		catch (Exception e) {
	//			e.printStackTrace();
	//		}
	//
	//		return set;
	//	}
	//
	//	public static void main(String[] args) {
	//		String path = "/Users/andre/Desktop/plugins/example.jar";
	//
	//		System.out.println(loadClasses(path));
	//	}


}
