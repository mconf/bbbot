package org.mconf.bbb.bot;

import java.util.HashMap;
import java.util.Map;

import com.beust.jcommander.IStringConverter;

class ProbabilitiesConverter implements IStringConverter<Map<Integer, Double>> {
	@Override
	public Map<Integer, Double> convert(String value) {
		Map<Integer, Double> map = new HashMap<Integer, Double>();
		String[] values = value.split(";");
		for (String tmp : values) {
			String[] pair = tmp.split(":");
			
			int k = Integer.parseInt(pair[0]);
			double v = Double.parseDouble(pair[1]);
			
			map.put(k, v);
		}
		return map;
	}
}