package com.niton;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map.Entry;

public class AutoMapper {
	public static boolean debug = false; 
	private static boolean match(String s1,String s2) {
		s1 = s1.toLowerCase();
		s2 = s2.toLowerCase();
		if(s1.equals(s2))
			return true;
		if(s1.contains(s2) || s2.contains(s1))
			return true;
		return false;
	}
	public static void map(
			Class<?> classToMap,
			Class<?> toMapTo,
			String output,
			String packageX
	) throws IOException {
		Field[] c1 = classToMap.getDeclaredFields();
		Field[] c2 = toMapTo.getDeclaredFields();
		
		
		HashMap<Field, Field> map = new HashMap<>();
		
		for (int i = 0; i < c1.length; i++) {
			Field f1 = c1[i];
			if(Modifier.isStatic(f1.getModifiers()))
				continue;
			for (int j = 0; j < c2.length; j++) {
				Field f2 = c2[j];
				if(Modifier.isStatic(f2.getModifiers()))
					continue;
				if(match(f1.getName(), f2.getName())) {
					map.put(f1, f2);
					System.out.println("Found match : "+f1+" -> "+f2);
				}
			}
		}
		
		MethodSpec.Builder convert1 = MethodSpec
				.methodBuilder("convert")
				.addModifiers(javax.lang.model.element.Modifier.PUBLIC,javax.lang.model.element.Modifier.STATIC)
				.addParameter(ParameterSpec.builder(classToMap, "from").build())
				.addParameter(ParameterSpec.builder(toMapTo, "to").build());
				
		if(map.size() != c1.length)
			convert1 = convert1.addAnnotation(Incomplete.class);
		
		for (Entry<Field, Field> fs : map.entrySet()) {
			Field f1 = fs.getKey();
			Field f2 = fs.getValue();
			String setExpression = Modifier.isPrivate(f1.getModifiers()) ? "to.set"+f1.getName().substring(0, 1).toUpperCase()+f1.getName().substring(1)+"(%GET%)" : "from."+f1.getName()+" = %GET%";
			String getExpression = Modifier.isPrivate(f2.getModifiers()) ? "from.get"+f2.getName().substring(0, 1).toUpperCase()+f2.getName().substring(1)+"()" : "from."+f2.getName();
			convert1 = convert1.addStatement(setExpression.replace("%GET%", getExpression));
		}
		MethodSpec.Builder convert2 = MethodSpec
				.methodBuilder("convert")
				.addModifiers(javax.lang.model.element.Modifier.PUBLIC,javax.lang.model.element.Modifier.STATIC)
				.addParameter(ParameterSpec.builder(toMapTo, "from").build())
				.addParameter(ParameterSpec.builder(classToMap, "to").build());
				
		if(map.size() != c1.length)
			convert2 = convert2.addAnnotation(Incomplete.class);
		
		for (Entry<Field, Field> fs : map.entrySet()) {
			Field f2 = fs.getKey();
			Field f1 = fs.getValue();
			String setExpression = Modifier.isPrivate(f1.getModifiers()) ? "to.set"+f1.getName().substring(0, 1).toUpperCase()+f1.getName().substring(1)+"(%GET%)" : "from."+f1.getName()+" = %GET%";
			String getExpression = Modifier.isPrivate(f2.getModifiers()) ? "from.get"+f2.getName().substring(0, 1).toUpperCase()+f2.getName().substring(1)+"()" : "from."+f2.getName();
			convert2 = convert2.addStatement(setExpression.replace("%GET%", getExpression));
		}
		
		
		
		for (int i = 0; i < c2.length; i++) {
			Field f2 = c2[i];
			if(!map.containsValue(f2))
				convert1 = convert1.addComment(f2.getName()+" was not found in other class");
		}
		for (int i = 0; i < c1.length; i++) {
			Field f1 = c1[i];
			if(!map.containsKey(f1))
				convert2 = convert2.addComment(f1.getName()+" was not found in other class");
		}
		
		
		
		if(debug) {
			System.out.println("Convert1 : ");
			System.out.println(convert1.build().toString());
			System.out.println("Convert2 : ");
			System.out.println(convert2.build().toString());
		}
		TypeSpec wire = TypeSpec
				.classBuilder(classToMap.getSimpleName()+"Wire")
				.addModifiers(javax.lang.model.element.Modifier.PUBLIC)
				.addMethod(convert1.build())
				.addMethod(convert2.build()).build();
		JavaFile file = JavaFile.builder(packageX, wire).build();
		file.writeTo(new File(output));
	}
}
