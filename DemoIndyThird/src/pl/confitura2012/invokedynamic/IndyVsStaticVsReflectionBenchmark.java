package pl.confitura2012.invokedynamic;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

public class IndyVsStaticVsReflectionBenchmark {
	public interface IExecutable {
		public long execute(long a, long b, int multiplier);
	}
	
	public static long sumAndMultiply(long a, long b, int multiplier) {
		return multiplier * (a + b);
	}

	public static Long sumAndMultiplyLong(Long a, Long b, int multiplier) {
		return multiplier * (a + b);
	}

	public static CallSite myBSM(MethodHandles.Lookup caller, String methodName, MethodType methodType, Object... bsmArgs) {
		java.lang.reflect.Method m = null;
		try {
			m = IndyVsStaticVsReflectionBenchmark.class.getMethod("sumAndMultiply", new Class[] { long.class, long.class, int.class });
			return new ConstantCallSite(caller.unreflect(m));
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return null;
	}

	
	public static void main(String args[]) throws Throwable {
		IExecutable obj = InvokeDynamic.prepareAs(	IExecutable.class,
													"run me", MethodType.methodType(long.class, long.class, long.class, int.class), 
													"myBSM", IndyVsStaticVsReflectionBenchmark.class, MethodType.methodType(CallSite.class, Lookup.class, String.class, MethodType.class, Object[].class));

		BenchmarkInvokeDynamic(obj);
		BenchmarkInvokeStatic();
		BenchmarkInvokeReflective();
		BenchmarkInvokeReflectiveNoBoxing();
	}
	
	
	final static long NUMBER_OF_LOOPS = 10_000;
	final static long NUMBER_OF_REPEATS = 5;
	final static int MULTIPLIER = 2;

	public static void BenchmarkInvokeDynamic(MethodHandle mh) throws Throwable
	{
		System.out.println("\nBenchmark INVOKE DYNAMIC (as MethodHandle)");
		for (int i = 0; i < NUMBER_OF_REPEATS; i++) {
			long start = System.currentTimeMillis();
			long sum = 0;
			for (long x = 0; x < NUMBER_OF_LOOPS; x++) {
				for (long y = 0; y < NUMBER_OF_LOOPS; y++) {
					sum += (long) mh.invokeExact(x, y, MULTIPLIER);
				}
			}
			System.out.println(sum + ", TIME: " + (System.currentTimeMillis() - start) + " ms");
		}
	}
	
	public static void BenchmarkInvokeDynamic(IExecutable exec)
	{
		System.out.println("\nBenchmark INVOKE DYNAMIC (as interface)");
		for (int i = 0; i < NUMBER_OF_REPEATS; i++) {
			long start = System.currentTimeMillis();
			long sum = 0;
			for (long x = 0; x < NUMBER_OF_LOOPS; x++) {
				for (long y = 0; y < NUMBER_OF_LOOPS; y++) {
					sum += exec.execute(x, y, MULTIPLIER);
				}
			}
			System.out.println(sum + ", TIME: " + (System.currentTimeMillis() - start) + " ms");
		}
	}

	public static void BenchmarkInvokeStatic()
	{
		System.out.println("\nBenchmark NORMAL (STATIC)");
		for (int i = 0; i < NUMBER_OF_REPEATS; i++) {
			long start = System.currentTimeMillis();
			long sum = 0;
			for (long x = 0; x < NUMBER_OF_LOOPS; x++) {
				for (long y = 0; y < NUMBER_OF_LOOPS; y++) {
					sum += sumAndMultiply(x, y, MULTIPLIER);
				}
			}
			System.out.println(sum + ", TIME: " + (System.currentTimeMillis() - start) + " ms");
		}
	}
	
	public static void BenchmarkInvokeReflective() throws Exception
	{
		System.out.println("\nBenchmark REFLECTIVE");
		
		Method sumAndMultiplyMethod = IndyVsStaticVsReflectionBenchmark.class.getDeclaredMethod("sumAndMultiply", new Class<?>[] { long.class, long.class, int.class });
		Object[] params = new Object[3];
		
		for (int i = 0; i < NUMBER_OF_REPEATS; i++) {
			long start = System.currentTimeMillis();
			long sum = 0;
			for (long x = 0; x < NUMBER_OF_LOOPS; x++) {
				for (long y = 0; y < NUMBER_OF_LOOPS; y++) {
					params[0] = x;
					params[1] = y;
					params[2] = MULTIPLIER;
					sum += ((long) sumAndMultiplyMethod.invoke(IndyVsStaticVsReflectionBenchmark.class, params));
				}
			}
			System.out.println(sum + ", TIME: " + (System.currentTimeMillis() - start) + " ms");
		}
	}

	public static void BenchmarkInvokeReflectiveNoBoxing() throws Exception
	{
		System.out.println("\nBenchmark REFLECTIVE NO BOXING");
		
		Method sumAndMultiplyMethod = IndyVsStaticVsReflectionBenchmark.class.getDeclaredMethod("sumAndMultiplyLong", new Class<?>[] { Long.class, Long.class, int.class });
		Object[] params = new Object[3];
		
		for (int i = 0; i < NUMBER_OF_REPEATS; i++) {
			long start = System.currentTimeMillis();
			Long sum = 0L;
			for (Long x = 0L; x < NUMBER_OF_LOOPS; x++) {
				for (Long y = 0L; y < NUMBER_OF_LOOPS; y++) {
					params[0] = x;
					params[1] = y;
					params[2] = MULTIPLIER;
					sum += ((Long) sumAndMultiplyMethod.invoke(IndyVsStaticVsReflectionBenchmark.class, params));
				}
			}
			System.out.println(sum + ", TIME: " + (System.currentTimeMillis() - start) + " ms");
		}
	}				
}