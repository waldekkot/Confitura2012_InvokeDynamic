package pl.confitura2012.helloindyworld;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.net.URL;

import pl.confitura2012.helloindyworld.InvokeDynamic.DynamicLoader;

public class TemplateForIndyCompleted {
	public interface IExecutable {
		public String execute(String s);
	}
		
	public static String myConfituraMethod(String s) {
		return s + " 2012! ";
	}
	
	public static CallSite myBSM(MethodHandles.Lookup caller, String methodName, MethodType methodType, Object... bsmArgs) {
		MethodHandle mh = null;
		try {
			mh = MethodHandles.lookup().findStatic(TemplateForIndyCompleted.class, "myConfituraMethod", 
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
					"myBSM", TemplateForIndyCompleted.class, MethodType.methodType(CallSite.class, Lookup.class, String.class, MethodType.class, Object[].class), "2013");

			
			System.out.println(mh.invoke("Confitura"));
			
//			IExecutable myObj = InvokeDynamic.prepareAsExecutable(HelloIndyWorld.class, dl,
//			"run me", MethodType.methodType(String.class, String.class), 
//			"myBSM", TemplateForIndy.class, MethodType.methodType(CallSite.class, Lookup.class, String.class, MethodType.class, Object[].class), "2013");			
		}
	}
	
	public static void main(String args[]) {
		try {
			DynamicLoader dl = new DynamicLoader(new URL[0]);
			Class<?> clazz = dl.loadClass(TemplateForIndyCompleted.class.getName() + "$" + HelloIndyWorld.class.getSimpleName());
			java.lang.reflect.Method m = clazz.getDeclaredMethod("main", new Class<?>[] { DynamicLoader.class });
			m.invoke(null, new Object[] { dl });
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
}