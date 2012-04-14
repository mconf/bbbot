package org.mconf.bbb.bot;

import com.beust.jcommander.IStringConverter;

public class BooleanConverter implements IStringConverter<Boolean> {
	@Override
	public Boolean convert(String value) {
		value = value.toLowerCase();
		return new Boolean(value.equals("true"));
	}
}