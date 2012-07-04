package pl.confitura2012.helloindyworld;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;

public class HelloInDyWorld1 {

	public static String myConfituraMethod(String s) {
		return s + " 2012";
	}

	public static CallSite myBSM(MethodHandles.Lookup caller, String methodName, MethodType methodType, Object... bsmArgs) {
		MethodHandle mh = null;
		try {
			mh = MethodHandles.lookup().findStatic(HelloInDyWorld1.class, "myConfituraMethod", 
					MethodType.methodType(String.class, String.class));
			
		} catch (NoSuchMethodException | IllegalAccessException e) {
			e.printStackTrace();
		}
		
		return new ConstantCallSite(mh);
	}
	
	public static void main(String args[]) throws Throwable {
		MethodHandle mh = InvokeDynamic.prepare("run me", MethodType.methodType(String.class, String.class), 
												"myBSM", HelloInDyWorld1.class, MethodType.methodType(CallSite.class, Lookup.class, String.class, MethodType.class, Object[].class));
		System.out.println(mh.invoke("Confitura"));

	}
}