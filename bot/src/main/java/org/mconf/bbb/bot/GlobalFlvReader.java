package org.mconf.bbb.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.flazr.io.flv.FlvAtom;
import com.flazr.rtmp.RtmpMessage;
import com.flazr.rtmp.RtmpReader;
import com.flazr.rtmp.message.Metadata;

public class GlobalFlvReader implements RtmpReader {
	
    private static final Logger logger = LoggerFactory.getLogger(GlobalFlvReader.class);

    private int frameIndex = 0;
    private final FlvPreLoader loader;
	
	public GlobalFlvReader(FlvPreLoader loader) {
		this.loader = loader;
	}

	@Override
	public Metadata getMetadata() {
		return loader.getReader().getMetadata();
	}

	@Override
	public RtmpMessage[] getStartMessages() {
		return loader.getReader().getStartMessages();
	}

	@Override
	public void setAggregateDuration(int targetDuration) {
	}

	@Override
	public long getTimePosition() {
		RtmpMessage message = loader.getMessages().get(frameIndex);
		return message.getHeader().getTime();
	}

	@Override
	public long seek(long timePosition) {
        logger.debug("trying to seek to: {}", timePosition);
        if(timePosition == 0) { // special case
        	frameIndex = 0;
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
		return frameIndex < loader.getMessages().size();
	}

	private boolean hasPrev() {
		return frameIndex > 0;
	}
	
	@Override
	public RtmpMessage next() {
		RtmpMessage message = loader.getMessages().get(frameIndex);
		frameIndex++;
		return new FlvAtom(message.getHeader().getMessageType(), message.getHeader().getTime(), message.encode().copy());
	}
	
	private RtmpMessage prev() {
		frameIndex--;
		RtmpMessage message = loader.getMessages().get(frameIndex);
		return new FlvAtom(message.getHeader().getMessageType(), message.getHeader().getTime(), message.encode().copy());
	}

	@Override
	public int getWidth() {
		return loader.getReader().getWidth();
	}

	@Override
	public int getHeight() {
		return loader.getReader().getHeight();
	}

}
