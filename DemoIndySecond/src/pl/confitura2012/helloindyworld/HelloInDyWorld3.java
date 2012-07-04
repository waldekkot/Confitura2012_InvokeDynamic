package pl.confitura2012.helloindyworld;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;

public class HelloInDyWorld3 {
	public interface IExecutable {
		public String execute(String s);
	}

	public static String myConfituraMethod(String s) { return s + " 2012"; }

	public static String myConfituraMethodNext(String s) { return s + " 2013"; }

	public static CallSite myBSM(MethodHandles.Lookup caller, String methodName, MethodType methodType, Object... bsmArgs) {
		MethodHandle mh = null;
		try {
			if((bsmArgs != null) && (bsmArgs.length > 0) && ("2013".equals(bsmArgs[0])))
				mh = MethodHandles.lookup().findStatic(HelloInDyWorld3.class, "myConfituraMethodNext", 
						MethodType.methodType(String.class, String.class));
			else
				mh = MethodHandles.lookup().findStatic(HelloInDyWorld3.class, "myConfituraMethod", 
						MethodType.methodType(String.class, String.class));
				
		} catch (NoSuchMethodException | IllegalAccessException e) {
			e.printStackTrace();
		}
		
		return new ConstantCallSite(mh);
	}
	
	public static void main(String args[]) throws Throwable {
		IExecutable myObj = InvokeDynamic.prepareAs(IExecutable.class,
													"run me", MethodType.methodType(String.class, String.class), 
													"myBSM", HelloInDyWorld3.class, MethodType.methodType(CallSite.class, Lookup.class, String.class, MethodType.class, Object[].class));
		System.out.println(myObj.execute("Hello !"));			
	}
}