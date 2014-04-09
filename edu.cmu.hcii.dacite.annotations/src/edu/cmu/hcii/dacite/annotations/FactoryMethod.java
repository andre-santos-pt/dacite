package edu.cmu.hcii.dacite.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * This annotation is to be used on public instance methods that belong to a Factory type (annotated with @Factory).
 * The return type of the method must be of a reference type and is implicitly assumed to be the type of the created object.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FactoryMethod {
	
}
