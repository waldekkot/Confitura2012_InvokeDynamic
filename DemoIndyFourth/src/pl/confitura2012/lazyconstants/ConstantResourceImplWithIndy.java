package pl.confitura2012.lazyconstants;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;

public class ConstantResourceImplWithIndy implements ConstantResource {
	private MethodHandle mh;
	
	public static CallSite bootstrapForCallMe(MethodHandles.Lookup callerClass, String name, java.lang.invoke.MethodType type, Object... args ) {		
		String xml = ParseHelper.doSomeHeavyParsing((String) args[0]);
		
		return new ConstantCallSite(MethodHandles.constant(String.class, xml));
	}

	public ConstantResourceImplWithIndy(String resourceName) {
		try {
			mh = InvokeDynamic.prepare("callMe", MethodType.methodType(String.class, new Class<?>[] {}), 
										"bootstrapForCallMe", ConstantResourceImplWithIndy.class, 
										MethodType.methodType(CallSite.class, Lookup.class, String.class, MethodType.class, Object[].class), 
						resourceName );
		} catch (Throwable e) {
			e.printStackTrace();
		}		
	}
	
	@Override
	public void notNeedingTheXMLHere() {
		//But, I am NOT using the 'xml' constant here !
		System.out.println("Called 'notNeedingTheXMLHere'");
	}

	@Override
	public void badlyNeedingTheXMLHere() {
		//I will be using the 'xml' constant here !
		System.out.println("Called 'notNeedingTheXMLHere'");
		try {
			System.out.println("XML: " + mh.invoke());
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

}
