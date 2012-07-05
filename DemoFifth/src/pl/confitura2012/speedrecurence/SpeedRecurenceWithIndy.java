package pl.confitura2012.speedrecurence;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.text.DecimalFormat;
import java.util.HashMap;

/**
 * This example demonstrates how using InvokeDynamic in a clever way, you can speed up a beautiful, but slow recursive code.
 * This example has been stolen from the <a href='http://code.google.com/p/jsr292-cookbook'>JSR292 Cookbook website</a>
 * and slightly adjusted to better suit as educational code during a Confitura 2012 conference talk about InvokeDynamic.
 * <p/>
 * Link to the original brilliant idea: <a href='http://code.google.com/p/jsr292-cookbook/source/browse/trunk/memoize/src/jsr292/cookbook/memoize/'>Memoize</a>
 * 
 * @author      Waldek Kot
 * @version     %I%, %G%
 */
public class SpeedRecurenceWithIndy {
	private static final long HOW_MANY_NUMBERS = 110;
	private static MethodHandle mhDynamic = null;

	public static long fib(long n) throws Throwable {
		if (n == 0)
			return 0;

		if (n == 1)
			return 1;

		// return fib(n - 1) + fib(n - 2);
		return (long) mhDynamic.invokeExact(n - 1) + (long) mhDynamic.invokeExact(n - 2);
	}
	
	//method invocation result's buffer
	private static ClassValue<HashMap<String, HashMap<Object, Object>>> cacheTables = new ClassValue<HashMap<String, HashMap<Object, Object>>>() {
		@Override
		protected HashMap<String, HashMap<Object, Object>> computeValue(Class<?> type) {
			return new HashMap<String, HashMap<Object, Object>>();
		}
	};		

	//helper methods
	public static boolean notNull(Object receiver) {
		return receiver != null;
	}

	public static Object update(HashMap<Object, Object> cache, Object result, Object arg) {
		cache.put(arg, result);
		return result;
	}

	private static final MethodHandle NOT_NULL;
	private static final MethodHandle MAP_GET;
	private static final MethodHandle UPDATE;
	static {
		Lookup lookup = MethodHandles.lookup();
		try {
			NOT_NULL = lookup.findStatic(SpeedRecurenceWithIndy.class, "notNull", MethodType.methodType(boolean.class, Object.class));
			MAP_GET = lookup.findVirtual(HashMap.class, "get", MethodType.methodType(Object.class, Object.class));
			UPDATE = lookup.findStatic(SpeedRecurenceWithIndy.class, "update",
					MethodType.methodType(Object.class, HashMap.class, Object.class, Object.class));

		} catch (ReflectiveOperationException e) {
			throw (AssertionError) new AssertionError().initCause(e);
		}
	}
	
	//warning ! stolen code ahead ! (http://code.google.com/p/jsr292-cookbook)
	public static CallSite myBSM(Lookup lookup, String name, MethodType type, Class<?> staticType) throws ReflectiveOperationException {
		MethodHandle target = lookup.findStatic(staticType, name, type);

		HashMap<String, HashMap<Object, Object>> cacheTable = cacheTables.get(staticType); 

		String selector = name + type.toMethodDescriptorString();
		HashMap<Object, Object> cache = cacheTable.get(selector);
		
		if (cache == null) {
			cache = new HashMap<Object, Object>(); 
			cacheTable.put(selector, cache); 
		}

		MethodHandle identity = MethodHandles.identity(type.returnType()); 
		identity = identity.asType(identity.type().changeParameterType(0, Object.class)); 
		identity = MethodHandles.dropArguments(identity, 1, type.parameterType(0)); 

		MethodHandle update = UPDATE.bindTo(cache);
		update = update.asType(type.insertParameterTypes(0, type.returnType())); 

		MethodHandle fallback = MethodHandles.foldArguments(update, target); 
		fallback = MethodHandles.dropArguments(fallback, 0, Object.class); 

		MethodHandle combiner = MethodHandles.guardWithTest(NOT_NULL, identity, fallback); 

		MethodHandle cacheQuerier = MAP_GET.bindTo(cache);
		cacheQuerier = cacheQuerier.asType(MethodType.methodType(Object.class, type.parameterType(0))); 

		MethodHandle memoize = MethodHandles.foldArguments(combiner, cacheQuerier);
		return new ConstantCallSite(memoize);
	}

	public static void main(String args[]) throws Throwable {
		System.out.println("SUM OF FIRST " + HOW_MANY_NUMBERS + " FIBONACCI SEQUENCE NUMBERS USING INVOKE DYNAMIC !");

		MethodType bsmType = MethodType.methodType(CallSite.class, Lookup.class, String.class, MethodType.class, Class.class);
		mhDynamic = InvokeDynamic.prepare("fib", MethodType.methodType(long.class, long.class),
											"myBSM", SpeedRecurenceWithIndy.class, bsmType,
											SpeedRecurenceWithIndy.class);
		long start = System.currentTimeMillis();		
		long result = 0L;
		for (long i = 0; i < HOW_MANY_NUMBERS; i++)
			result +=  (long) mhDynamic.invokeExact(i);
		
		System.out.println("TIME: "  + (System.currentTimeMillis() - start) + " ms");
		System.out.println("RESULT: " + new DecimalFormat("###,###,###,###,###").format(result));
	}
}