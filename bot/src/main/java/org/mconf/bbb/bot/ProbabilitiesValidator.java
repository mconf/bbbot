package org.mconf.bbb.bot;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

public class ProbabilitiesValidator implements IParameterValidator {
	@Override
	public void validate(String name, String value)
			throws ParameterException {
		String[] values = value.split(";");
		double acc = 0.0;
		for (String tmp : values) {
			String[] pair = tmp.split(":");
			if (pair.length != 2)
				throw new ParameterException("Parameter " + name + " should be composed by semicolon (;) separated pairs [number of users]:[probability] (found invalid pair " + tmp +")");
			try {
				Integer.parseInt(pair[0]);
				double v = Double.parseDouble(pair[1]);
				acc += v;
			} catch (NumberFormatException e) {
				throw new ParameterException("Parameter " + name + " should be composed by semicolon (;) separated pairs [number of users]:[probability] (found invalid pair " + tmp +")");
			}
		}
		if (Math.ceil(acc) != 100.0) {
			throw new ParameterException("Parameter " + name + " should sum 100% (found " + acc + "%)");
		}
	}
}