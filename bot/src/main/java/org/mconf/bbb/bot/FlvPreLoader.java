package org.mconf.bbb.bot;

import java.util.ArrayList;
import java.util.List;

import com.flazr.io.flv.FlvReader;
import com.flazr.rtmp.RtmpMessage;

public class FlvPreLoader {

    private final FlvReader reader;
	private final List<RtmpMessage> messages;

	public FlvPreLoader(final String path) {
		reader = new FlvReader(path);
		messages = new ArrayList<RtmpMessage>();
		while (reader.hasNext()) {
			messages.add(reader.next());
		}
		reader.close();
	}
	
	public final List<RtmpMessage> getMessages() {
		return messages;
	}
	
	public final FlvReader getReader() {
		return reader;
	}

}
