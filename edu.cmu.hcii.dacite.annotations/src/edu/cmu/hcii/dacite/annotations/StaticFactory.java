package edu.cmu.hcii.dacite.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation marks a method as a Static Factory, and can only be used on public static methods.
 * The return type of the method must be of a reference type and is implicitly assumed to be the type of the created object.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface StaticFactory {
	
}
