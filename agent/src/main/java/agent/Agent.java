package agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

import java.lang.reflect.Field;

public class Agent {

	/**
	 * This is called before the standard main
	 * method and initializes a ClassFileTransformer
	 * which allows injection and recompilation of each  
	 * class file as it is requested by the JVM. 
	 *
	 * @param agentArgs  this should be a list of comma
	 *                   separated package prefixes to trace
	 * @param inst       provides access/control of JVM
	 */
	public static void premain(String agentArgs, Instrumentation inst) {

		String temp[] = agentArgs.split("###");
		final String appDir = temp[0];
		final String args[] = temp[1].split(",");

		/* Set ProfileLogger.APP_DIR or Fail */
		try {
			Field field = ProfileLogger.class.getDeclaredField("APP_DIR");
			field.set(null, appDir);
		} catch (Exception e) {
			System.err.println(e);
			System.err.println("No such field...");
			System.exit(-1);
		}

		inst.addTransformer(new ClassFileTransformer() {

			@Override
			public byte[] transform(ClassLoader classLoader, String s, Class<?> aClass, ProtectionDomain protectionDomain, byte[] bytes) throws IllegalClassFormatException {

				try {

					ClassPool cp = ClassPool.getDefault();

					final CtClass logClass = cp.get("agent.ProfileLogger");
					final String className = s.replaceAll("/", ".");
					final CtClass thisClass = cp.get(className);

					/* filter by package */
					for (String pack : args) {

						if (!className.startsWith("java.") && !className.startsWith("agent.") && className.startsWith(pack + ".")) {

							CtMethod[] methods = thisClass.getDeclaredMethods​();

							for (CtMethod m : methods) {

								if (!isTraceable(m)) {
								  continue;
								}

								final String methodName = m.getLongName();


								/* log calls to Thread.start() */
								m.instrument(new ExprEditor() {

									@Override
									public void edit(MethodCall m) {
										try {
											
											String callName = m.getMethod().getLongName();

											if (callName.equals("java.lang.Thread.start()")) {

												m.replace(

													// logThreadStart(Thread.getID())
													"{ agent.ProfileLogger.getInstance().logThreadStart($0.getId());" + 

													// original call
													"$_ = $proceed($$);}"
												);

											}

					 					} catch (Exception ex) {
											ex.printStackTrace();
										}
									}

								});

								// ProfileLogger logger
								m.addLocalVariable("logger", logClass);

								// long elapsed time
								m.addLocalVariable("LOG_START_TIME", CtClass.longType);

								// start timer & get logger
								m.insertBefore("{" +
								  "logger = agent.ProfileLogger.getInstance();" +
								  "logger.logMethodStart(\"" + methodName + "\");" +
								  "LOG_START_TIME = System.nanoTime();}");

								// end timer & log
								m.insertAfter(
									"{logger.logMethodDuration(\"" + methodName + "\", System.nanoTime() - LOG_START_TIME);}");

							}

							/* recompile */
							byte[] byteCode = thisClass.toBytecode();
							thisClass.detach();
							return byteCode;

						}

					}

				} catch (Exception ex) {
					ex.printStackTrace();
				}

				return null;
			}
		});
	}

	/**
	 * Asks whether we are able to trace this method.
	 * Some, mostly native, methods cannot be transformed.
	 *
	 * @param method  method to trace
	 */
	public static boolean isTraceable(CtMethod method) {
	  return !(Modifier.isNative(method.getModifiers()) || Modifier.isAbstract(method.getModifiers()));
	}

}

