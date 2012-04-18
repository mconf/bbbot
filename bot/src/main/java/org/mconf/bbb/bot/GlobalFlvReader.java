package org.mconf.bbb.bot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.flazr.io.flv.FlvReader;
import com.flazr.rtmp.RtmpMessage;
import com.flazr.rtmp.RtmpReader;
import com.flazr.rtmp.message.Metadata;

public class GlobalFlvReader implements RtmpReader {
	
    private static final Logger logger = LoggerFactory.getLogger(GlobalFlvReader.class);

    private final FlvReader reader;
	private final List<RtmpMessage> messages;
	private final Map<Long, Integer> indexMap;
	
	public GlobalFlvReader(final String path) {
		reader = new FlvReader(path);
		messages = new ArrayList<RtmpMessage>();
		while (reader.hasNext()) {
			messages.add(reader.next());
		}
		reader.close();
		indexMap = new HashMap<Long, Integer>();
	}

	private synchronized int getMessageIndex() {
		long threadId = Thread.currentThread().getId();
		Integer index = indexMap.get(threadId);
		if (index == null) {
			indexMap.put(threadId, 0);
			index = 0;
		}
//		logger.debug("getMessageIndex: {} -> {}", threadId, index);
		return index;
	}
	
	private synchronized void setMessageIndex(int index) {
		long threadId = Thread.currentThread().getId();
		indexMap.put(threadId, index);
//		logger.debug("setMessageIndex: {} -> {}", threadId, index);
	}
	
	@Override
	public Metadata getMetadata() {
		return reader.getMetadata();
	}

	@Override
	public RtmpMessage[] getStartMessages() {
		return reader.getStartMessages();
	}

	@Override
	public void setAggregateDuration(int targetDuration) {
	}

	@Override
	public long getTimePosition() {
		RtmpMessage message = messages.get(getMessageIndex());
		return message.getHeader().getTime();
	}

	@Override
	public long seek(long timePosition) {
        logger.debug("trying to seek to: {}", timePosition);
        if(timePosition == 0) { // special case
        	setMessageIndex(0);
        	return 0;
        }
        final long start = getTimePosition();        
        if(timePosition > start) {
            while(hasNext()) {
                final RtmpMessage cursor = next();
                if(cursor.getHeader().getTime() >= timePosition) {                    
                    break;
                }
            }
        } else {
            while(hasPrev()) {
                final RtmpMessage cursor = prev();
                if(cursor.getHeader().getTime() <= timePosition) {
                    next();
                    break;
                }
            }
        }
        return getTimePosition();
	}

	@Override
	public void close() {
	}

	@Override
	public boolean hasNext() {
        logger.debug("=======================> {} hasNext", Thread.currentThread().getId());
		return getMessageIndex() < messages.size();
	}

	private boolean hasPrev() {
		return getMessageIndex() > 0;
	}
	
	@Override
	public RtmpMessage next() {
		int index = getMessageIndex();
		RtmpMessage message = messages.get(index);
		setMessageIndex(index + 1);
        logger.debug("=======================> {} -> {}", Thread.currentThread().getId(), message.getHeader().getTime());
		return message;
	}
	
	private RtmpMessage prev() {
		int index = getMessageIndex() - 1;
		RtmpMessage message = messages.get(index);
		setMessageIndex(index);
		return message;
	}

	@Override
	public int getWidth() {
		return reader.getWidth();
	}

	@Override
	public int getHeight() {
		return reader.getHeight();
	}

}
