package org.mconf.bbb.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.flazr.io.flv.FlvWriter;
import com.flazr.rtmp.RtmpMessage;

public class LimitedSizeFlvWriter extends FlvWriter {
	private static final Logger log = LoggerFactory.getLogger(LimitedSizeFlvWriter.class);
	
	private int size;
	private boolean callbackCalled = false;
	private long acc = 0, count = 0;
	
	private long previous_pkt_timestamp, actual_pkt_timestamp, previous_timestamp, actual_timestamp;

	public LimitedSizeFlvWriter(String fileName, int size) {
		super(fileName);
		this.size = size;
	}
	
	@Override
	public void write(RtmpMessage message) {
		if (message.getHeader().getTime() <= size) {
			actual_pkt_timestamp = message.getHeader().getTime();
			actual_timestamp = System.currentTimeMillis();
			if (actual_pkt_timestamp == 0) {
				// first packet of the stream
				
			} else {
				long diff_pkt_timestamp = actual_pkt_timestamp - previous_pkt_timestamp;
				long diff_timestamp = actual_timestamp - previous_timestamp;
				long t = Math.abs(diff_timestamp - diff_pkt_timestamp);
				acc += t;
				count += 1;
				log.debug("diff_pkt_timestamp: {} | diff_timestamp: {}", diff_pkt_timestamp, diff_timestamp);
			}
			previous_pkt_timestamp = actual_pkt_timestamp;
			previous_timestamp = actual_timestamp;
			super.write(message);
		} else if (!callbackCalled) {
			onFinished();
			callbackCalled = true;
		}
	}
	
	@Override
	public void close() {
		super.close();
	}
	
	protected void onFinished() {
		log.info("Recording done!");
		log.info("Average jitter: {} ms", acc / count);
	}
}
