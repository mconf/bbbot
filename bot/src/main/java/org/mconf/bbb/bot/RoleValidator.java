package org.mconf.bbb.bot;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

public class RoleValidator implements IParameterValidator {
	@Override
	public void validate(String name, String value)
			throws ParameterException {
		String lower_value = value.toLowerCase();
		if (!lower_value.equals("viewer") && !lower_value.equals("moderator"))
			throw new ParameterException("Parameter " + name + " should be [viewer] or [moderator] (found " + value +")");
	}
}