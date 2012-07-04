package pl.confitura2012.invokedynamic;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URL;
import java.net.URLClassLoader;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * InvokeDynamic class allows to use the JSR-292's (aka 'InvokeDynamic') dynamic method invocations mechanism from a regular Java code.
 * The public methods in the class generate (using ASM bytecode generator) a new class with a single method which contains 
 * InvokeDynamic bytecode instruction. The InvokeDynamic instruction is configured (e.g. its BSM) with the given parameters.
 * Regular Java code can then call the generated method and in the end the dynamic method invocation is performed.
 * <p/>
 * Access to the dynamic call is given in two forms as: <ol> 
 * <li> MethodHandle (using the method: {@link InvokeDynamic#prepare}) </li>  
 * <li> instance of given functional interface (using the method: {@link InvokeDynamic#prepareAs}) </li></ol>
 * <p/>
 * The code below is for educational purposes (e.g. used during the Confitura 2012 conference: http://confitura.pl).
 * The code is based on the brilliant examples from various sources, including:
 * - http://code.google.com/p/jsr292-cookbook
 * - http://nerds-central.blogspot.com 
 * 
 * @author      Waldek Kot
 * @version     %I%, %G%
 */
public class InvokeDynamic {
	private static final String NAME_OF_CLASS_WITH_INVOKEDYNAMIC = "ClassWithIndy";
	private static final String NAME_OF_METHOD_WITH_INVOKEDYNAMIC = "MethodWithInvokeDynamic";
	private static int dynamicCallNumber = 0;

	public static class DynamicLoader extends URLClassLoader {
		public DynamicLoader(URL[] urls) {
			super(urls);
		}

		public Class<?> loadFromBytes(byte[] classDefinition) {
			Class<?> clazz = defineClass(null, classDefinition, 0, classDefinition.length);
			resolveClass(clazz);
			return clazz;
		}
	}

	/**
	 * Prepares a new InvokeDynamic bytecode instruction which when invoked will invoke the specified method in a dynamic way. 
	 *
	 * @param	methodName Name of the method being invoked, as defined at the method's use (i.e. at the call site)
	 * @param	methodType Types of the method's return value and parameters (if any) 
	 * @param	bsmName Name of the call site's <a href='http://docs.oracle.com/javase/7/docs/api/java/lang/invoke/package-summary.html'>bootstrap method</a>
	 * @param	bsmClass Name of the class in which the bootstrap method is located 
	 * @param	bsmType Types of the bootstrap method's return value and parameters
	 * @param	bsmParams Optional parameters passed from the call site to the bootstrap method
	 * @return	Handle (reference) to the generated method containing the invokedynamic instruction  
	 */	
	public static MethodHandle prepare(	String methodName, MethodType methodType, 
										String bsmName,	Class<?> bsmClass, MethodType bsmType, Object... bsmParams) throws Throwable {
		byte[] classFile = generateClassWithIndyAsStatic(methodName, methodType, bsmName, bsmClass, bsmType, bsmParams);
				
		Class<?> indyClass = null;
		MethodHandle mh = null;
		try(DynamicLoader dl = new DynamicLoader(new URL[0])) {
			indyClass = dl.loadFromBytes(classFile);
			mh = MethodHandles.lookup().findStatic(indyClass, NAME_OF_METHOD_WITH_INVOKEDYNAMIC, methodType);
		}
		
		return mh;
	}

	/**
	 * Prepares a new InvokeDynamic bytecode instruction which when invoked will invoke the specified method in a dynamic way. 
	 *
	 * @param	interfaceClass Functional interface which the returned object will implement
	 * @param	methodName Name of the method being invoked, as defined at the method's use (i.e. at the call site)
	 * @param	methodType Types of the method's return value and parameters (if any) 
	 * @param	bsmName Name of the call site's <a href='http://docs.oracle.com/javase/7/docs/api/java/lang/invoke/package-summary.html'>bootstrap method</a>
	 * @param	bsmClass Name of the class in which the bootstrap method is located 
	 * @param	bsmType Types of the bootstrap method's return value and parameters
	 * @param	bsmParams Optional parameters passed from the call site to the bootstrap method
	 * @return	Object implementing the given functional interface (i.e. with a single method). The method's implementation contains the InvokeDynamic bytecode.  
	 */	
	public static <T> T prepareAs(	Class<T> interfaceClass,
									String methodName, MethodType methodType, 
									String bsmName,	Class<?> bsmClass, MethodType bsmType, Object... bsmParams) throws Throwable {
		byte[] classFile = generateClassWithIndyAsInterface(interfaceClass, methodName, methodType, bsmName, bsmClass, bsmType, bsmParams);

		Class<?> indyClass = null;
		T obj = null;
		try(DynamicLoader dl = new DynamicLoader(new URL[0])) {
			indyClass = dl.loadFromBytes(classFile);
			
			MethodHandle mh = MethodHandles.lookup().findConstructor(indyClass, MethodType.methodType(void.class));
			obj = (T) mh.invoke();
		}
		return obj;
	}

	/**
	 * Generates a new class (using <a href='http://asm.ow2.org'>ASM</a> bytecode generator) with: <ul>
	 * <li> a default, public constructor </li>
	 * <li> a single, public static method, containing the InvokeDynamic bytecode instruction</li> 
	 *
	 * @param	methodName Name of the public, static method 
	 * @param	methodType Types of the method's return value and parameters (if any) 
	 * @param	bsmName Name of the bootstrap method (for the INVOKEDYNAMIC bytecode instruction)
	 * @param	bsmClass Name of the class in which the bootstrap method is located 
	 * @param	bsmType Types of the bootstrap method's return value and parameters
	 * @param	bsmParams Optional parameters to the bootstrap method
	 * @return	bytecode of the generated class  
	 */		
	private static byte[] generateClassWithIndyAsStatic(String methodName, MethodType methodType, 
														String bsmName, Class<?> bsmClass,	MethodType bsmType, Object... bsmArgs) throws Exception {
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS + ClassWriter.COMPUTE_FRAMES);

		generateClass(cw, bsmClass.getPackage().getName());

		generateParameterlessConstructor(cw);

		generateMethodWithInDy(cw, methodName, methodType, bsmName, bsmClass, bsmType, bsmArgs);

		cw.visitEnd();		
		return cw.toByteArray();
	}

	/**
	 * Generates a new class (using <a href='http://asm.ow2.org'>ASM</a> bytecode generator) implementing the given single method interface.
	 * The class will have: <ul>
	 * <li> a default, public constructor </li>
	 * <li> a single, public method (implementation of the interface), containing the InvokeDynamic bytecode instruction</li></ul>
	 * 
	 * @param	executableInterface Functional interface which the class will implement 
	 * @param	methodName Name of the public, static method 
	 * @param	methodType Types of the method's return value and parameters (if any) 
	 * @param	bsmName Name of the bootstrap method (for the INVOKEDYNAMIC bytecode instruction)
	 * @param	bsmClass Name of the class in which the bootstrap method is located 
	 * @param	bsmType Types of the bootstrap method's return value and parameters
	 * @param	bsmParams Optional parameters to the bootstrap method
	 * @return	bytecode of the generated class  
	 */		
	private static byte[] generateClassWithIndyAsInterface( Class<?> executableInterface, 
															String methodName, MethodType methodType, 
															String bsmName,	Class<?> bsmClass, MethodType bsmType, Object... bsmArgs) throws Exception {
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS + ClassWriter.COMPUTE_FRAMES);

		generateClassWithInterface(cw, bsmClass.getPackage().getName(), executableInterface);

		generateParameterlessConstructor(cw);

		generateMethodWithIndyAsInterfaceImplementation(cw, executableInterface, methodName, methodType, bsmName, bsmClass, bsmType, bsmArgs);

		cw.visitEnd();

		return cw.toByteArray();
	}

	/**
	 * Generates the basic section of the class structure (class name, modifiers, etc.)
	 * The class will be 'put' in the give package.
	 *
	 * @param	cw ASM's ClassWriter object 
	 * @param	packageName Java package of the generated class  
	 */		
	private static void generateClass(ClassWriter cw, String packageName) {
		String className = packageName.replace('.', '/') + "/" + 
				NAME_OF_CLASS_WITH_INVOKEDYNAMIC + dynamicCallNumber;
		cw.visit(Opcodes.V1_7, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, className, null, "java/lang/Object", null);

		dynamicCallNumber++;
	}
	
	/**
	 * Generates the basic section of the class structure (class name, its package, modifiers, etc.). 
	 * The class will implement the given interface.
	 * The class will also be 'put' in the give package.
	 *
	 * @param	cw ASM's ClassWriter object used to generate a class structure
	 * @param	packageName Java package of the generated class  
	 * @param	executableInterface Interface which the generated class will implement  
	 */		
	private static void generateClassWithInterface(ClassWriter cw, String packageName, Class<?> executableInterface) {
		String className = packageName.replace('.', '/') + "/" + 
				NAME_OF_CLASS_WITH_INVOKEDYNAMIC + dynamicCallNumber;
		String[] namesOfInterfaces = new String[] { executableInterface.getName().replace('.', '/') };

		cw.visit(Opcodes.V1_7, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, className, null, "java/lang/Object", namesOfInterfaces);

		dynamicCallNumber++;
	}
	
	/**
	 * Generates the default, public constructor. 
	 *
	 * @param	cw ASM's ClassWriter object used to generate a class structure
	 */		
	private static void generateParameterlessConstructor(ClassWriter cw) {
		MethodVisitor mv;

		mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
		mv.visitCode();
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
		mv.visitInsn(Opcodes.RETURN);		
		mv.visitMaxs(-1, -1);		
		mv.visitEnd();
	}

	/**
	 * Generates a method (public, static) and in its implementation inserts INVOKEDYNAMIC bytecode instruction.  
	 *
	 * @param	cw ASM's ClassWriter object used to generate a class structure
	 * @param	methodName Name of the generated method
	 * @param	methodType Signature of the generated method
	 * @param	bsmName Name of the bootstrap method for the INVOKEDYNAMIC bytecode
	 * @param	bsmClass Class in which the bootstrap method is located
	 * @param	bsmType Signature of the bootstrap method
	 * @param	bsmArgs Optional parameters to the bootstrap method
	 */		
	private static void generateMethodWithInDy(ClassWriter cw, 
											String methodName, MethodType methodType, 
											String bsmName, Class<?> bsmClass,	MethodType bsmType, Object... bsmArgs) {
		MethodVisitor mv;

		mv = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, 
							NAME_OF_METHOD_WITH_INVOKEDYNAMIC, methodType.toMethodDescriptorString(), 
							null, null);
		manageMethodArgumentsStatic(mv, methodType.toMethodDescriptorString(), bsmArgs);

		manageBSMArguments(bsmArgs);

		mv.visitCode();

		generateInvokeDynamicInstruction(mv, methodName, methodType, bsmName, bsmClass, bsmType, bsmArgs);

		manageMethodResult(mv, methodType.returnType());

		mv.visitMaxs(-1, -1);		
		mv.visitEnd();
	}	
	
	/**
	 * Generates a method (implementation of the given functional interface's method) and in its implementation inserts INVOKEDYNAMIC bytecode instruction.  
	 *
	 * @param	cw ASM's ClassWriter object used to generate a class structure
	 * @param	executableInterface Interface with a single method 
	 * @param	methodName Name of the generated method
	 * @param	methodType Signature of the generated method
	 * @param	bsmName Name of the bootstrap method for the INVOKEDYNAMIC bytecode
	 * @param	bsmClass Class in which the bootstrap method is located
	 * @param	bsmType Signature of the bootstrap method
	 * @param	bsmArgs Optional parameters to the bootstrap method
	 */		
	private static void generateMethodWithIndyAsInterfaceImplementation(ClassWriter cw, Class<?> executableInterface, 
																		String methodName, MethodType methodType, 
																		String bsmName, Class<?> bsmClass, MethodType bsmType, Object... bsmArgs) {
		MethodVisitor mv;

		String theInterfaceMethodName = executableInterface.getDeclaredMethods()[0].getName();
		String theInterfaceMethodType = Type.getMethodDescriptor(executableInterface.getDeclaredMethods()[0]);

		mv = cw.visitMethod(Opcodes.ACC_PUBLIC, theInterfaceMethodName, theInterfaceMethodType, null, null);
		manageMethodArgumentsInterface(mv, theInterfaceMethodType, bsmArgs);

		manageBSMArguments(bsmArgs);

		mv.visitCode();

		generateInvokeDynamicInstruction(mv, methodName, methodType, bsmName, bsmClass, bsmType, bsmArgs);

		manageMethodResult(mv, methodType.returnType());

		mv.visitMaxs(-1, -1);
		mv.visitEnd();
	}
	
	/**
	 * Manages space in the local variables part of the method's frame for the static method arguments  
	 *
	 * @param	mv ASM's MethodVisitor object used to generate a method structure
	 * @param	methodName Name of the generated method
	 * @param	bsmArgs Optional parameters to the bootstrap method
	 */		
	private static void manageMethodArgumentsStatic(MethodVisitor mv, String methodDescriptorString, Object... bsmArgs) {
		int slot = 0;
		for (Type parameterType : Type.getArgumentTypes(methodDescriptorString)) {
			mv.visitVarInsn(parameterType.getOpcode(Opcodes.ILOAD), slot);
			slot += parameterType.getSize();
		}
	}

	/**
	 * Manages space in the local variables part of the method's frame for the interface method arguments  
	 *
	 * @param	mv ASM's MethodVisitor object used to generate a method structure
	 * @param	methodName Name of the generated method
	 * @param	bsmArgs Optional parameters to the bootstrap method
	 */		
	private static void manageMethodArgumentsInterface(MethodVisitor mv, String theInterfaceMethodType, Object... bsmArgs) {
		int slot = 1;
		for (Type parameterType : Type.getArgumentTypes(theInterfaceMethodType)) {
			mv.visitVarInsn(parameterType.getOpcode(Opcodes.ILOAD), slot);
			slot += parameterType.getSize();
		}
	}
	
	/**
	 * Manages types of the bootstrap methods arguments (the BSM can have many legal declarations).   
	 *
	 * @param	bsmArgs Optional parameters to the bootstrap method
	 */		
	private static void manageBSMArguments(Object... bsmArgs) {
		if (bsmArgs == null)
			return;

		for (int i = 0; i < bsmArgs.length; i++) {
			Object bsmArg = bsmArgs[i];
			if (bsmArg instanceof Class<?>) {
				bsmArgs[i] = Type.getType((Class<?>) bsmArg);
				continue;
			}
	        if (bsmArg instanceof MethodType) {
	        	bsmArgs[i] = Type.getType(((MethodType) bsmArg).toMethodDescriptorString());
	            continue;
	        }
	        if (bsmArg instanceof MethodHandle) {
	        	continue;
	        }			
		}
	}

	/**
	 * Generates INVOKEDYNAMIC bytecode instruction.  
	 *
	 * @param	mv ASM's MethodVisitor object used to generate a method structure
	 * @param	methodName Name of the callsite
	 * @param	methodType Signature of the callsite
	 * @param	bsmName Name of the bootstrap method 
	 * @param	bsmClass Class in which the bootstrap method is located
	 * @param	bsmType Signature of the bootstrap method
	 * @param	bsmArgs Optional parameters to the bootstrap method
	 */		
	private static void generateInvokeDynamicInstruction(	MethodVisitor mv, 
															String methodName, MethodType methodType, 
															String bsmName,	Class<?> bsmClass, MethodType bsmType, Object... bsmArgs) {
		Handle bootstrap = new Handle(Opcodes.H_INVOKESTATIC, bsmClass.getName().replace('.', '/'), bsmName, bsmType.toMethodDescriptorString());
		mv.visitInvokeDynamicInsn(methodName, methodType.toMethodDescriptorString(), bootstrap, bsmArgs);
	}
	
	/**
	 * Manages space for the method's result (returned value, if any).  
	 *
	 * @param	mv ASM's MethodVisitor object used to generate a method structure
	 * @param	returnType Type of the method's result
	 */		
	private static void manageMethodResult(MethodVisitor mv, Class<?> returnType) {
		if (void.class.equals(returnType)) {
			mv.visitInsn(Opcodes.RETURN);
		} else if (double.class.equals(returnType)) {
			mv.visitInsn(Opcodes.DRETURN);
		} else if (float.class.equals(returnType)) {
			mv.visitInsn(Opcodes.FRETURN);
		} else if ((int.class.equals(returnType) || (boolean.class.equals(returnType)) || (char.class.equals(returnType))
				|| (short.class.equals(returnType)) || (byte.class.equals(returnType)))) {
			mv.visitInsn(Opcodes.IRETURN);
		} else if (long.class.equals(returnType)) {
			mv.visitInsn(Opcodes.LRETURN);
		} else {
			mv.visitInsn(Opcodes.ARETURN);
		}
	}
}