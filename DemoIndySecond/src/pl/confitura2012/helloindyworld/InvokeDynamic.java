package pl.confitura2012.helloindyworld;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import pl.confitura2012.helloindyworld.TemplateForIndy.IExecutable;

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

	// creates a class with a method containing the invokedynamic instruction
	// with specified arguments
	// returns a MethodHandle to the method with the invokedynamic instruction
	public static MethodHandle prepare(	Class<?> forClass, DynamicLoader dl, 
										String methodName, MethodType methodType, String bsmName,
										Class<?> bsmClass, MethodType bsmType, Object... bsmParams) throws Throwable {
		byte[] classFile = dumpAsStatic(forClass, methodName, methodType, bsmName, bsmClass, bsmType, bsmParams);

		Class<?> indyClass = dl.loadFromBytes(classFile);

		MethodHandle mh = MethodHandles.lookup().findStatic(indyClass, NAME_OF_METHOD_WITH_INVOKEDYNAMIC, methodType);

		return mh;
	}

	// creates a class with a method containing the invokedynamic instruction
	// with specified arguments
	// returns an object with a method (as defined in the IExecutable interface)
	// with the invokedynamic instruction
	public static IExecutable prepareAsExecutable(	Class<?> forClass, DynamicLoader dl, 
													String methodName, MethodType methodType, String bsmName,
													Class<?> bsmClass, MethodType bsmType, Object... bsmParams) throws Throwable {
		byte[] classFile = dumpAsInterface(forClass, IExecutable.class, methodName, methodType, bsmName, bsmClass, bsmType, bsmParams);

		Class<?> indyClass = dl.loadFromBytes(classFile);
		Constructor<?> cons = indyClass.getConstructor(new Class<?>[0]);
		return (IExecutable) cons.newInstance(new Object[0]);
	}

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

	private static void generateClassWithInterface(ClassWriter cw, Class<?> forClass, Class<?> executableInterface) {
		String className = forClass.getEnclosingClass().getPackage().getName().replace('.', '/') + "/" + NAME_OF_CLASS_WITH_INVOKEDYNAMIC + dynamicCallNumber;
		String[] namesOfInterfaces = new String[] { executableInterface.getName().replace('.', '/') };

		cw.visit(Opcodes.V1_7, 
				Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, 
				className, 
				null, 
				"java/lang/Object", 
				namesOfInterfaces);

		dynamicCallNumber++;
	}

	private static void generateClass(ClassWriter cw, Class<?> forClass) {
		String className = forClass.getEnclosingClass().getPackage().getName().replace('.', '/') + "/"	+ NAME_OF_CLASS_WITH_INVOKEDYNAMIC + dynamicCallNumber;
		cw.visit(Opcodes.V1_7, 
				Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, 
				className, 
				null, 
				"java/lang/Object", 
				null);

		dynamicCallNumber++;
	}

	private static void generateInvokeDynamicInstruction(MethodVisitor mv, String methodName, MethodType methodType, String bsmName, Class<?> bsmClass, MethodType bsmType, Object... bsmArgs) {
		Handle bootstrap = new Handle(Opcodes.H_INVOKESTATIC, bsmClass.getName().replace('.', '/'), bsmName, bsmType.toMethodDescriptorString());
		mv.visitInvokeDynamicInsn(methodName, methodType.toMethodDescriptorString(), bootstrap, bsmArgs);
	}

	private static void manageMethodArgumentsInterface(MethodVisitor mv, String theInterfaceMethodType, Object... bsmArgs) {
		int slot = 1;
		for (Type parameterType : Type.getArgumentTypes(theInterfaceMethodType)) {
			mv.visitVarInsn(parameterType.getOpcode(Opcodes.ILOAD), slot);
			slot += parameterType.getSize();
		}
	}

	private static void manageMethodArgumentsStatic(MethodVisitor mv, String methodDescriptorString, Object... bsmArgs) {
		int slot = 0;
		for (Type parameterType : Type.getArgumentTypes(methodDescriptorString)) {
			mv.visitVarInsn(parameterType.getOpcode(Opcodes.ILOAD), slot);
			slot += parameterType.getSize();
		}
	}
	
	private static void manageBSMArguments(Object... bsmArgs) {
		if(bsmArgs == null)
			return;
		
		for (int i = 0; i < bsmArgs.length; i++) {
			Object bsmArg = bsmArgs[i];
			if (bsmArg instanceof Class<?>) {
				bsmArgs[i] = Type.getType((Class<?>) bsmArg);
				continue;
			}
		}		
	}
	
	private static void manageMethodResult(MethodVisitor mv, Class<?> returnType) {		
		if (void.class.equals(returnType)) {
			mv.visitInsn(Opcodes.RETURN);
		} else
		if (double.class.equals(returnType)) {
			mv.visitInsn(Opcodes.DRETURN);
		} else
		if (float.class.equals(returnType)) {
			mv.visitInsn(Opcodes.FRETURN);
		} else
		if ((int.class.equals(returnType) || (boolean.class.equals(returnType)) || (char.class.equals(returnType)) || (short.class.equals(returnType))
				|| (byte.class.equals(returnType)))) {
			mv.visitInsn(Opcodes.IRETURN);
		} else
		if (long.class.equals(returnType)) {
			mv.visitInsn(Opcodes.LRETURN);
		} else {
			mv.visitInsn(Opcodes.ARETURN);
		}		
	}

	private static void generateMainMethodAsInterfaceImplementation(ClassWriter cw, Class<?> executableInterface, 
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

		mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

		manageMethodResult(mv, methodType.returnType());
		
		mv.visitMaxs(-1, -1);
		mv.visitEnd();
	}
		
	private static void generateMainMethod(	ClassWriter cw, 
											String methodName, MethodType methodType,
											String bsmName, Class<?> bsmClass, MethodType bsmType, Object... bsmArgs) {
		MethodVisitor mv;

		mv = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, NAME_OF_METHOD_WITH_INVOKEDYNAMIC, methodType.toMethodDescriptorString(), null, null);

		manageMethodArgumentsStatic(mv, methodType.toMethodDescriptorString(), bsmArgs);

		manageBSMArguments(bsmArgs);

		mv.visitCode();

		generateInvokeDynamicInstruction(mv, methodName, methodType, bsmName, bsmClass, bsmType, bsmArgs);

		mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

		manageMethodResult(mv, methodType.returnType());
		
		mv.visitMaxs(-1, -1);
		mv.visitEnd();
	}

	private static byte[] dumpAsStatic(	Class<?> forClass, 
										String methodName, MethodType methodType,
										String bsmName, Class<?> bsmClass, MethodType bsmType, Object... bsmArgs) throws Exception {
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

		generateClass(cw, forClass);

		generateParameterlessConstructor(cw);

		generateMainMethod(cw, methodName, methodType, bsmName, bsmClass, bsmType, bsmArgs);

		cw.visitEnd();
		return cw.toByteArray();
	}
	
	private static byte[] dumpAsInterface(	Class<?> forClass, Class<?> executableInterface, 
											String methodName, MethodType methodType,
											String bsmName, Class<?> bsmClass, MethodType bsmType, Object... bsmArgs) throws Exception {
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		
		generateClassWithInterface(cw, forClass, executableInterface);
		
		generateParameterlessConstructor(cw);
		
		generateMainMethodAsInterfaceImplementation(cw, executableInterface, methodName, methodType, bsmName, bsmClass, bsmType, bsmArgs);
		
		cw.visitEnd();			
		return cw.toByteArray();
	}	
}