package agent;

import java.io.File;
import java.io.PrintStream;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.lang.StringBuilder;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;

import java.lang.StackTraceElement;

public class ProfileLogger {

	public static String APP_DIR;
	private static final String OUTFILE = "/out/thread_";
	private static final String OUTFILE_EXT = ".txt";

	private static final String ENS = "UTF8";

	private static Map<Long,ProfileLogger> logMap = Collections.synchronizedMap(new HashMap<Long,ProfileLogger>());

	private PrintStream out;

	/**
	 * Using a synchronized Map, fetches/creates
	 * new instances based on unique key of the
	 * thread id of current Thread.
	 *
	 * @return inst  appropriate instance
	 */
	public static ProfileLogger getInstance() {
		long tid = Thread.currentThread().getId();
		ProfileLogger inst = null;

		synchronized (logMap) {
			if (!logMap.containsKey(tid)) {
				inst = new ProfileLogger(tid);
				logMap.put(tid, inst);
			} else {
				inst = logMap.get(tid);
			}
		}

		return inst;
	}

	/**
	 * Creates an instance with a corresponding output
	 * file. This is a private constructor, only called
	 * by ProfileLogger.getInstance().
	 *
	 * @param tid  thread-id to include in file name
	 */
	private ProfileLogger(long tid) {

		String fileName = APP_DIR + OUTFILE + String.valueOf(tid) + OUTFILE_EXT;

		File file;

		try {

			file = new File(fileName);
			file.getParentFile().mkdirs();
			this.out = new PrintStream(file, ENS);

		} catch (SecurityException e) {
			System.err.println("SecurityException!");
			System.exit(-1);
		} catch (FileNotFoundException e) {
			System.err.println(fileName + " not found.");
			System.exit(-1);
		} catch (UnsupportedEncodingException e) {
			System.err.println(ENS + " not found.");
			System.exit(-1);
		}

	}

	/**
	 * Logs the start of a new Thread in the parent
	 * Thread including the thread id instead of the
	 * duration.
	 * The format remains the same as a method start/end
	 * for parsing simplicity.
	 */
	public void logThreadStart(long tid) {
		String indent = getIndent();
		this.out.println(indent + "\tThread.start() : (" + getCaller(3) + ")");
		this.out.println(indent + "\tThread.start() : " + String.valueOf(tid));
	}

	/**
	 * Logs the start of a method call.
	 */
	public void logMethodStart(String methodSig) {
		this.out.println(getIndent() + methodSig + " : (" + getCaller(4) + ")");
	}

	/**
	 * Logs the end of a method call and its duration.
	 */
	public void logMethodDuration(String methodSig, long duration) {
		this.out.println(getIndent() + methodSig + " : " + String.valueOf(duration));
	}

	/**
	 * Get the caller File/linenum of a stack trace
	 */
	public static String getCaller(int index) {

		StackTraceElement[] trace = Thread.currentThread().getStackTrace();

		if (trace.length > index) {
			StackTraceElement e = trace[index];
			return e.getFileName() + " : " + e.getLineNumber();
		} else {
			// main method
			return "null : 0";
		}
	}

	/**
	 * Creates an indentation String based on the
	 * current thread's stack trace.
	 *
	 * @return indent  String containing appropriate
	 *                 amount of tab characters 
	 */
	private static String getIndent() {
		int level = Thread.currentThread().getStackTrace().length - 4;

		StringBuilder indent = new StringBuilder();
		for (int i = 0; i < level; i++) {
			indent.append('\t');
		}

		return indent.toString();
	}
}