package pl.confitura2012.helloindyworld;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.net.URL;

import pl.confitura2012.helloindyworld.InvokeDynamic.DynamicLoader;

public class TemplateForIndy {
	public interface IExecutable {
		public String execute(String s);
	}

	public static String myConfituraMethod(String s) {
		return s + " 2012";
	}

	public static String myConfituraMethodNext(String s) {
		return s + " 2013";
	}

	public static CallSite myBSM(MethodHandles.Lookup caller, String methodName, MethodType methodType, Object... bsmArgs) {
		MethodHandle mh = null;
		try {
			if((bsmArgs != null) && (bsmArgs.length > 0) && ("2013".equals(bsmArgs[0])))
				mh = MethodHandles.lookup().findStatic(TemplateForIndy.class, "myConfituraMethodNext", 
						MethodType.methodType(String.class, String.class));
			else
				mh = MethodHandles.lookup().findStatic(TemplateForIndy.class, "myConfituraMethod", 
						MethodType.methodType(String.class, String.class));
				
		} catch (NoSuchMethodException | IllegalAccessException e) {
			e.printStackTrace();
		}
		
		return new ConstantCallSite(mh);
	}
	
	public static class HelloIndyWorld {
		public static void main(DynamicLoader dl) throws Throwable {
			MethodHandle mh = InvokeDynamic.prepare(HelloIndyWorld.class, dl,
					"run me", MethodType.methodType(String.class, String.class), 
					"myBSM", TemplateForIndy.class, MethodType.methodType(CallSite.class, Lookup.class, String.class, MethodType.class, Object[].class));
			System.out.println(mh.invoke("Confitura"));

			MethodHandle mh2 = InvokeDynamic.prepare(HelloIndyWorld.class, dl,
					"run me two", MethodType.methodType(String.class, String.class), 
					"myBSM", TemplateForIndy.class, MethodType.methodType(CallSite.class, Lookup.class, String.class, MethodType.class, Object[].class), "2013");
			System.out.println(mh2.invoke("Confitura"));
			
			IExecutable myObj = InvokeDynamic.prepareAsExecutable(HelloIndyWorld.class, dl,
					"run me", MethodType.methodType(String.class, String.class), 
					"myBSM", TemplateForIndy.class, MethodType.methodType(CallSite.class, Lookup.class, String.class, MethodType.class, Object[].class));
			System.out.println(myObj.execute("Hello !"));			
		}
	}
	
	public static void main(String args[]) {
		try {
			DynamicLoader dl = new DynamicLoader(new URL[0]);
			Class<?> clazz = dl.loadClass(TemplateForIndy.class.getName() + "$" + HelloIndyWorld.class.getSimpleName());
			java.lang.reflect.Method m = clazz.getDeclaredMethod("main", new Class<?>[] { DynamicLoader.class });
			m.invoke(null, new Object[] { dl });
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
}