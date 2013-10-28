
package edu.uthscsa.ric.papaya.builder;

import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;


public class YuiCompressorErrorReporter implements ErrorReporter {

	@Override
	public void warning(String message, String sourceName, int line, String lineSource, int lineOffset) {
		if (line < 0) {
			System.err.println("Warning: " + message);
		} else {
			System.err.println("Warning: " + line + ':' + lineOffset + ':' + message);
		}
	}



	@Override
	public void error(String message, String sourceName, int line, String lineSource, int lineOffset) {
		if (line < 0) {
			System.err.println("Error: " + message);
		} else {
			System.err.println("Error: " + line + ':' + lineOffset + ':' + message);
		}
	}



	@Override
	public EvaluatorException runtimeError(String message, String sourceName, int line, String lineSource, int lineOffset) {
		error(message, sourceName, line, lineSource, lineOffset);
		return new EvaluatorException(message);
	}
}
