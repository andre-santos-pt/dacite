package edu.cmu.hcii.dacite.annotations.processor;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import com.sun.mirror.apt.AnnotationProcessor;
import com.sun.mirror.apt.AnnotationProcessorEnvironment;
import com.sun.mirror.apt.AnnotationProcessorFactory;
import com.sun.mirror.apt.Filer;
import com.sun.mirror.declaration.AnnotationTypeDeclaration;
import com.sun.mirror.declaration.ClassDeclaration;
import com.sun.mirror.declaration.ConstructorDeclaration;
import com.sun.mirror.declaration.MethodDeclaration;
import com.sun.mirror.declaration.Modifier;
import com.sun.mirror.declaration.ParameterDeclaration;
import com.sun.mirror.declaration.TypeDeclaration;
import com.sun.mirror.declaration.TypeParameterDeclaration;
import com.sun.mirror.type.AnnotationType;
import com.sun.mirror.type.ArrayType;
import com.sun.mirror.type.ClassType;
import com.sun.mirror.type.DeclaredType;
import com.sun.mirror.type.EnumType;
import com.sun.mirror.type.InterfaceType;
import com.sun.mirror.type.PrimitiveType;
import com.sun.mirror.type.ReferenceType;
import com.sun.mirror.type.TypeMirror;
import com.sun.mirror.type.TypeVariable;
import com.sun.mirror.type.VoidType;
import com.sun.mirror.type.WildcardType;
import com.sun.mirror.util.SimpleTypeVisitor;
import com.sun.mirror.util.TypeVisitor;

import edu.cmu.hcii.dacite.annotations.Builder;
import edu.cmu.hcii.dacite.annotations.Parent;
import edu.cmu.hcii.dacite.annotations.Factory;
import edu.cmu.hcii.dacite.annotations.FactoryMethod;
import edu.cmu.hcii.dacite.annotations.Helper;
import edu.cmu.hcii.dacite.annotations.Helpers;
import edu.cmu.hcii.dacite.annotations.Decorate;
import edu.cmu.hcii.dacite.annotations.StaticFactories;
import edu.cmu.hcii.dacite.annotations.StaticFactory;


public class ApiProcessorFactory implements AnnotationProcessorFactory {

	@Override
	public Collection<String> supportedOptions() {
		return Collections.emptyList();
	}

	@Override
	public Collection<String> supportedAnnotationTypes() {
		Collection<String> types = new ArrayList<>();
		types.add(Factory.class.getPackage().getName() + ".*");
		return types;
	}

	@Override
	public AnnotationProcessor getProcessorFor(Set<AnnotationTypeDeclaration> atds, AnnotationProcessorEnvironment env) {
		return new Processor(env);
	}



	private static class Processor implements AnnotationProcessor {

		private AnnotationProcessorEnvironment env;

		public Processor(AnnotationProcessorEnvironment env) {
			this.env = env;
		}

		@Override
		public void process() {
			for(TypeDeclaration d : env.getTypeDeclarations()) {
				File file = new File(d.getQualifiedName());
				try {
					PrintWriter writer = env.getFiler().createTextFile(Filer.Location.SOURCE_TREE, "",file, "UTF-8");

					if(d.getAnnotation(Factory.class) != null)
						handleFactory(env, writer, d);

					if(d.getAnnotation(StaticFactories.class) != null) {

						StaticFactories ann = d.getAnnotation(StaticFactories.class);
						for(String type : ann.value())
							handleStaticFactories(env, writer, d, type);
					}

					if(d.getAnnotation(Helpers.class) != null) {
						Helpers ann = d.getAnnotation(Helpers.class);
						for(String type : ann.value())
							handleHelpers(env, writer, d, type);
					}

					for(MethodDeclaration m : d.getMethods()) {

						if(m.getAnnotation(StaticFactory.class) != null)
							handleStaticFactory(env, writer, m);

						else if(m.getAnnotation(FactoryMethod.class) != null)
							handleFactoryMethod(env, writer, m);

						else if(m.getAnnotation(Builder.class) != null)
							if(d instanceof ClassDeclaration) {
								for(ConstructorDeclaration c : ((ClassDeclaration)d).getConstructors()) {
									handleBuilder(env, writer, c, m);
								}
							}

						checkHelper(env, writer, m);
					}

					if(d instanceof ClassDeclaration) {
						for(ConstructorDeclaration c : ((ClassDeclaration)d).getConstructors()) {
							checkDecorator(env, writer, c);
							//checkParent(env, writer, c);
						}
					}

					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}





		private void handleFactory(AnnotationProcessorEnvironment env, PrintWriter writer, TypeDeclaration dec) {
			Set<String> types = new HashSet<>();
			for(MethodDeclaration m : dec.getMethods()) {
				if(m.getAnnotation(FactoryMethod.class) != null) {
					types.add(getType(m.getReturnType(), false));
				}
			}

			String typeList = "";
			for(String t : types) {
				if(!typeList.isEmpty())
					typeList += ",";

				typeList += t;
			}

			writer.println(Factory.class.getSimpleName() + ":" + typeList);
		}


		private void handleStaticFactories(AnnotationProcessorEnvironment env, PrintWriter writer, TypeDeclaration d, String type) throws IOException {
			for(MethodDeclaration m : d.getMethods()) {
				if(getType(m.getReturnType(), false).equals(type))
					handleStaticFactory(env, writer, m);
			}
		}




		// FactoryMethod:TargetType:MethodName:MethodParams:ReturnType
		private void handleStaticFactory(AnnotationProcessorEnvironment env, PrintWriter writer, MethodDeclaration m) throws IOException {
			if(!m.getModifiers().contains(Modifier.PUBLIC) || !m.getModifiers().contains(Modifier.STATIC))
				env.getMessager().printError(m.getPosition(), "A static factory must be public and static.");
			else {
				writer.println(getLine(StaticFactory.class.getSimpleName(), m.getReturnType(), m));
			}
		}



		private void handleFactoryMethod(AnnotationProcessorEnvironment env, PrintWriter writer, MethodDeclaration m) throws IOException {
			if(m.getDeclaringType().getAnnotation(Factory.class) == null)
				env.getMessager().printError(m.getPosition(), "A factory method must be inside of a factory type (annotated with @Factory)");
			else if(!m.getModifiers().contains(Modifier.PUBLIC) || m.getModifiers().contains(Modifier.STATIC))
				env.getMessager().printError(m.getPosition(), "A factory method must be public and non-static.");
			else {
				writer.println(getLine(FactoryMethod.class.getSimpleName(), m.getReturnType(), m));
			}
		}


		private void handleBuilder(AnnotationProcessorEnvironment env, PrintWriter writer, ConstructorDeclaration c, MethodDeclaration m) throws IOException {
			if(!m.getModifiers().contains(Modifier.PUBLIC) || m.getModifiers().contains(Modifier.STATIC))
				env.getMessager().printError(m.getPosition(), "A builder method must be public and non-static.");
			else if(!m.getParameters().isEmpty())
				env.getMessager().printError(m.getPosition(), "A builder method cannot have parameters.");
			else {
				String line = Builder.class.getSimpleName() + ":" +  
						m.getReturnType() + ":" + 
						m.getSimpleName() + ":" + 
						commaSeparatedParams( c.getParameters()) + ":" +
						getType(m.getReturnType(), false);

				writer.println(line);
			}
		}

		private void handleHelpers(AnnotationProcessorEnvironment env, PrintWriter writer, TypeDeclaration d, String type) throws IOException {
			for(MethodDeclaration m : d.getMethods()) {
				List<ParameterDeclaration> params = new ArrayList<>(m.getParameters());
				for(int i = 0; i < params.size(); i++) {
					ParameterDeclaration p = params.get(i);
					TypeMirror typeMirror  = p.getType();
					if(getType(typeMirror, false).equals(type)) {
						String kind = m.getModifiers().contains(Modifier.STATIC) ? "static" : "instance";
						writer.println(getLine(Helper.class.getSimpleName(), p.getType(), m) + ":" + i + ":" + kind);
						break;
					}
				}
			}
		}


		// Helper:TargetType:MethodName:MethodParams:ReturnType
		private void checkHelper(AnnotationProcessorEnvironment env, PrintWriter writer, MethodDeclaration m) throws IOException {
			List<ParameterDeclaration> params = new ArrayList<>(m.getParameters());
			boolean set = false;

			for(int i = 0; i < params.size(); i++) {
				ParameterDeclaration p = params.get(i);
				if(p.getAnnotation(Helper.class) != null) {
					if(set)
						env.getMessager().printError(p.getPosition(), "The annotation @Helper can only placed in at most on one parameter of the method.");
					else if(!m.getModifiers().contains(Modifier.PUBLIC))
						env.getMessager().printError(m.getPosition(), "A helper method should be public.");
					else if(!set){
						String kind = m.getModifiers().contains(Modifier.STATIC) ? "static" : "instance";
						writer.println(getLine(Helper.class.getSimpleName(), p.getType(), m) + ":" + i + ":" + kind);
						set = true;
					}
				}
			}
		}


		private void checkDecorator(AnnotationProcessorEnvironment env, PrintWriter writer, ConstructorDeclaration c) throws IOException {
			List<ParameterDeclaration> params = new ArrayList<>(c.getParameters());
			boolean set = false;

			for(int i = 0; i < params.size(); i++) {
				ParameterDeclaration p = params.get(i);
				if(p.getAnnotation(Parent.class) != null || p.getAnnotation(Decorate.class) != null) {
					String annType =  "@" + p.getAnnotation(Parent.class) != null ? Parent.class.getSimpleName() : Decorate.class.getSimpleName();
					if(set)
						env.getMessager().printError(p.getPosition(), "The annotation " + annType  + " can only placed in at most on one parameter of the constructor.");
					else if(!c.getModifiers().contains(Modifier.PUBLIC))
						env.getMessager().printError(c.getPosition(), "The constructor should be public.");
					else if(!set){
						String line = Parent.class.getSimpleName() + ":" +  
								getType( p.getType(), false) + ":" + 
								"new" + ":" + 
								commaSeparatedParams(c.getParameters()) + ":" +
								getType(p.getType(), false);
						writer.println(line + ":" + i);
						set = true;
					}
				}
			}
		}

		private void checkParent(AnnotationProcessorEnvironment env, PrintWriter writer, ConstructorDeclaration c) throws IOException {
			List<ParameterDeclaration> params = new ArrayList<>(c.getParameters());
			boolean set = false;

			for(int i = 0; i < params.size(); i++) {
				ParameterDeclaration p = params.get(i);
				if(p.getAnnotation(Parent.class) != null) {

					if(set)
						env.getMessager().printError(p.getPosition(), "The annotation @Compose can only placed in at most on one parameter of the constructor.");
					else if(!c.getModifiers().contains(Modifier.PUBLIC))
						env.getMessager().printError(c.getPosition(), "The constructor should be public.");
				}
				else {
					String line = Decorate.class.getSimpleName() + ":" +  
							getType( p.getType(), false) + ":" + 
							"new" + ":" + 
							commaSeparatedParams(c.getParameters()) + ":" +
							getType(p.getType(), false);
					writer.println(line + ":" + i);
				}
			}
		}


		private String getLine(String header, TypeMirror type, MethodDeclaration m) throws IOException {
			return header + ":" +  
					getType(type, false) + ":" + 
					m.getSimpleName() + ":" + 
					commaSeparatedParams(m.getParameters()) + ":" +
					getType(m.getReturnType(), false);
		}



		private String commaSeparatedParams(Collection<ParameterDeclaration> params) {
			String s = "";
			for(ParameterDeclaration d : params) {
				if(!s.isEmpty())
					s+=",";

				s+=getType(d.getType(), false) + " " + d.getSimpleName();
			}

			return s.isEmpty() ? " " : s;
		}

		private String getType(TypeMirror type, boolean includeTypeArgs) {
			if(type instanceof ArrayType) 
				return getType(((ArrayType)type).getComponentType(), includeTypeArgs) + "[]";
			else
				return getNonArrayType(type, includeTypeArgs);
		}
		
		private String getNonArrayType(TypeMirror type, boolean includeTypeArgs) {
			if(type instanceof DeclaredType) {
				TypeDeclaration dec = ((DeclaredType) type).getDeclaration();
				Collection<TypeParameterDeclaration> typeParams = dec.getFormalTypeParameters();
				String s = dec.getQualifiedName();

				if(includeTypeArgs && !typeParams.isEmpty()) {
					s += "<";

					for(TypeParameterDeclaration p : typeParams) {
						if(s.endsWith("*"))
							s +=",";
						s += "*";
					}
					s += ">";
				}
				return s;
			}
			else if (type instanceof TypeVariable)
				return "Object";
			else if (type instanceof VoidType)
				return "void";
			else if (type instanceof PrimitiveType)
				return ((PrimitiveType) type).getKind().name().toLowerCase();

			return null;
		}








		private String stripComments(String javadoc) {
			StringBuffer buffer = new StringBuffer();
			Scanner scanner = new Scanner(javadoc);
			while(scanner.hasNextLine()) {
				String line = scanner.nextLine().trim();
				if(line.endsWith("*/"))
					line = line.substring(0, line.length()-2);
				else if(line.startsWith("/*") && line.length() > 2) {
					line = line.substring("/*".length());
				}

				if(line.startsWith("*")) {
					if(line.length() > 1)
						line = line.substring(1);
					else
						line = "";
				}

				if(!line.isEmpty())
					buffer.append(line + "<BR>");
			}

			scanner.close();
			return buffer.toString().trim();
		}
	}
}
