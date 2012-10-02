package org.mconf.bbb.bot;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.flazr.io.flv.FlvWriter;
import com.flazr.rtmp.RtmpMessage;

public class LimitedSizeFlvWriter extends FlvWriter {
	private static final Logger log = LoggerFactory.getLogger(LimitedSizeFlvWriter.class);
	
	private int size;
	private long interval_acc = 0, interval_count = 0, delta = 0;
	private long total_acc = 0, total_count = 0;
	private long transmission_acc = 0, transmission_count = 0;
	private static final long JITTER_INTERVAL = 1000;
	private FlvWriter writer;
	
	private long previous_pkt_timestamp, actual_pkt_timestamp, previous_timestamp, actual_timestamp, pkt_timestamp_delta = 0;
	private boolean first_pkt = true;

	public LimitedSizeFlvWriter(String fileName, int size) {
		super(fileName);
		this.size = size;
	}
	
	private void startWriter() {
		SimpleDateFormat format = new SimpleDateFormat("HH-mm-ss-SSS");
		String filename = format.format(new Date()) + ".flv";
		log.info(filename);
		writer = new FlvWriter(filename);
	}
	
	private void stopWriter() {
		writer.close();
	}
	
	@Override
	public void write(RtmpMessage message) {
		actual_pkt_timestamp = message.getHeader().getTime();
		actual_timestamp = System.currentTimeMillis();
		if (first_pkt) {
			startWriter();
			pkt_timestamp_delta = actual_pkt_timestamp;
			first_pkt = false;
		}
		
		if (actual_pkt_timestamp == 0) {
			// first packet of the stream
			
		} else {
			long diff_pkt_timestamp = actual_pkt_timestamp - previous_pkt_timestamp;
			long diff_timestamp = actual_timestamp - previous_timestamp;
			long t = Math.abs(diff_timestamp - diff_pkt_timestamp);
			total_acc += t; interval_acc += t; transmission_acc += t;
			total_count += 1; interval_count += 1; transmission_count += 1;
			log.debug("actual_pkt_timestamp:{} | diff_pkt_timestamp: {} | actual_timestamp: {} | diff_timestamp: {}", new Object[] {actual_pkt_timestamp, diff_pkt_timestamp, actual_timestamp, diff_timestamp});
			
			if (actual_pkt_timestamp - delta >= JITTER_INTERVAL) {
				delta = actual_pkt_timestamp;
				float avg = interval_acc / (float) interval_count;
				log.info("Average jitter: {} ms{}", avg, avg >= diff_pkt_timestamp? " ***** POSSIBLE PROBLEM HERE! *****": "");
				interval_acc = 0;
				interval_count = 0;
			}
		}
		previous_pkt_timestamp = actual_pkt_timestamp;
		previous_timestamp = actual_timestamp;
		
		if (actual_pkt_timestamp - pkt_timestamp_delta <= size) {
			writer.write(message);
		} else {
			onFinished();
			first_pkt = true;
			stopWriter();
		}	
	}
	
	@Override
	public void close() {
		log.info("Average jitter of the whole transmission: {} ms",  total_acc / (float) total_count);
		writer.close();
		super.close();
	}
	
	protected void onFinished() {
		log.info("Recording done!");
		log.info("Average jitter of the recorded transmission: {} ms",  transmission_acc / (float) transmission_count);
		transmission_acc = 0;
		transmission_count = 0;
	}
}
