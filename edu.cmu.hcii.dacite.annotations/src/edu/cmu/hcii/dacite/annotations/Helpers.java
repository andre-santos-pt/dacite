package edu.cmu.hcii.dacite.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is to be used in public static methods, with at least one parameter of a reference type.
 * 
 * rules...
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Helpers {
	String[] value();
}
