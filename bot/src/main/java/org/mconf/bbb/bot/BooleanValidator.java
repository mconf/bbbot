package org.mconf.bbb.bot;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

public class BooleanValidator implements IParameterValidator {
	@Override
	public void validate(String name, String value)
			throws ParameterException {
		String lower_value = value.toLowerCase();
		if (!lower_value.equals("true") && !lower_value.equals("false"))
			throw new ParameterException("Parameter " + name + " should be [true] or [false] (found " + value +")");
	}
}