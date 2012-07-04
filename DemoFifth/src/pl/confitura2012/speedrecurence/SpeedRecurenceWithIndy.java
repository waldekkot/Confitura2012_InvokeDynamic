package pl.confitura2012.speedrecurence;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.text.DecimalFormat;

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
	private static final long HOW_MANY_NUMBERS = 40;
	private static MethodHandle mhDynamic = null;

	public static long fib(long n) throws Throwable {
		if (n == 0)
			return 0;

		if (n == 1)
			return 1;

		// return fib(n - 1) + fib(n - 2);
		return (long) mhDynamic.invokeExact(n - 1) + (long) mhDynamic.invokeExact(n - 2);
	}

	public static CallSite myBSM(Lookup lookup, String name, MethodType type, Class<?> staticType) throws ReflectiveOperationException {
		MethodHandle mh = null;
		try {
			mh = MethodHandles.lookup().findStatic(SpeedRecurenceWithIndy.class, "fib", type);
		} catch (NoSuchMethodException | IllegalAccessException e) {
			e.printStackTrace();
		}
		return new ConstantCallSite(mh);
	}

	public static void main(String args[]) throws Throwable {
		System.out.println("SUM OF FIRST " + HOW_MANY_NUMBERS + " FIBONACCI SEQUENCE NUMBERS USING INVOKE DYNAMIC !");
		MethodType bsmType = MethodType.methodType(CallSite.class, Lookup.class, String.class, MethodType.class, Class.class);
		mhDynamic = InvokeDynamic.prepare("fib", MethodType.methodType(long.class, long.class), 
											"myBSM", SpeedRecurenceWithIndy.class, bsmType,
											SpeedRecurenceWithIndy.class);

		long start = System.currentTimeMillis();
		long result = 0;
		for (long i = 0; i < HOW_MANY_NUMBERS; i++) {
			result += (long) mhDynamic.invokeExact(i);
		}
		System.out.println("RESULT: " + new DecimalFormat("###,###,###,###,###").format(result));
		System.out.println("TIME: " + (System.currentTimeMillis() - start) + " ms");
	}
}