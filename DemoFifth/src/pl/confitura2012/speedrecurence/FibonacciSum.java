package pl.confitura2012.speedrecurence;

import java.text.DecimalFormat;

/**
 * This example demonstrates how using InvokeDynamic in a clever way, you can speed up a beautiful, but slow recursive code.
 * This example has been stolen from the <a href='http://code.google.com/p/jsr292-cookbook'>JSR292 Cookbook website</a>
 * and slightly adjusted to better suit as educational code during a Confitura 2012 conference talk about InvokeDynamic.
 * <p/>
 * Link to the original brilliant idea: <a href='http://code.google.com/p/jsr292-cookbook/source/browse/trunk/memoize/src/jsr292/cookbook/memoize/'>Memoize</a>
 * <p/>
 * The code calculates sum of a first N (e.g. 40) numbers of the Fibonacci sequence.
 * The code uses recursion, which makes it beautiful, but rather inefficient (at least in Java code).
 * It is inefficient, because the 'fib' procedure is executed multiple times with the same argument (forgetting the previously calculated numbers).
 * So, it is doing way too much calculations.
 * The idea is to make the invocations of the 'fib' procedure dynamic (using facilities of the JSR-292 InvokeDynamic dynamic method invocation).
 * 
 * @author      Waldek Kot
 * @version     %I%, %G%
 */
public class FibonacciSum {
	private static final long HOW_MANY_NUMBERS = 40;

	public static long fib(long n) {
		if (n == 0)
			return 0;

		if (n == 1)
			return 1;

		return fib(n - 1) + fib(n - 2);
	}

	public static void main(String[] args) {
		System.out.println("SUM OF FIRST " + HOW_MANY_NUMBERS + " NUMBERS OF FIBONACCI SEQUENCE USING RECURRENCE !");

		long start = System.currentTimeMillis();
		long result = 0;
		for (long i = 0; i < HOW_MANY_NUMBERS; i++) 
			result += fib(i);

		System.out.println("TIME: " + (System.currentTimeMillis() - start) + " ms");
		System.out.println("RESULT: " + new DecimalFormat("###,###,###,###,###").format(result));
	}
}
