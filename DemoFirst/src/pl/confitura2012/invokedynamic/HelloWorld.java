package pl.confitura2012.invokedynamic;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

public class HelloWorld {

	public static void main(String[] args) throws Throwable {
		MethodHandle mh = MethodHandles.constant(String.class, "Hello InDy World !");
		
		System.out.println(mh.invoke());

	}
}
