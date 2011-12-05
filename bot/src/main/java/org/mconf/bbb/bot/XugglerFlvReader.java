package org.mconf.bbb.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.flazr.rtmp.RtmpMessage;
import com.flazr.rtmp.RtmpReader;
import com.flazr.rtmp.message.Metadata;
import com.flazr.rtmp.message.MetadataAmf0;
import com.flazr.rtmp.message.Video;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;

public class XugglerFlvReader implements RtmpReader {

	private static final Logger log = LoggerFactory.getLogger(XugglerFlvReader.class);
	private IContainer container;
	private IStreamCoder videoCoder;
	private int videoStreamId;
	private String filename;
	private IPacket nextPacket = null;
	private Metadata metadata;
	
	public XugglerFlvReader(String filename) throws Exception {
		this.filename = filename;
		container = IContainer.make();
		
		if (!openContainer())
			throw new Exception("Could not read file");
		
		int numStreams = container.getNumStreams();
		videoStreamId = -1;
		videoCoder = null;
		for (int i=0; i < numStreams; i++) {
			IStream stream = container.getStream(i);
			IStreamCoder coder = stream.getStreamCoder();
			
			log.info("Stream " + i + ": " + coder.getCodecType() + "(" + coder.getCodecID() + ")");

			if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO) {
				videoStreamId = i;
				videoCoder = coder;
			}
		}
		if (videoStreamId == -1)
			throw new Exception("There are no video streams on this file");
		findNext();
		
		metadata = new MetadataAmf0("onMetaData");
		metadata.setDuration(container.getDuration());
		metadata.setValue("width", getWidth());
		metadata.setValue("height", getHeight());
	}
	
	private boolean openContainer() {
		return !(container.open(filename, IContainer.Type.READ, null) < 0);
	}

	public int getWidth() { return videoCoder.getWidth(); }
	public int getHeight() { return videoCoder.getHeight(); }
	
	@Override
	public void close() {
		container.close();
	}

	@Override
	public Metadata getMetadata() {
		return metadata;
	}

	@Override
	public RtmpMessage[] getStartMessages() {
		Video[] startMessages = new Video[0];
        return startMessages;
	}

	@Override
	public long getTimePosition() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean hasNext() {
		return (nextPacket != null);
	}

	private void findNext() {
		nextPacket = null;
		
		IPacket packet = IPacket.make();
		while(container.readNextPacket(packet) >= 0) {
			if (packet.getStreamIndex() == videoStreamId) {
				nextPacket = packet;
				break;
			}	
		}
	}
	
	@Override
	public RtmpMessage next() {
		if (nextPacket == null)
			return new Video();
		
		nextPacket.acquire();

		int timestamp = (int) nextPacket.getTimeStamp();
		
    	int offset = 0;
		int bufferSize = nextPacket.getData().getBufferSize() - offset;

		byte[] buffer = new byte[bufferSize + 1];
		if (nextPacket.isKey())
			buffer[0] = 0x32;
		else
			buffer[0] = 0x12;
		
		System.arraycopy(nextPacket.getData().getByteArray(offset, bufferSize), 0, buffer, 1, bufferSize);
		bufferSize += 1;
		
		Video videoMessage = new Video(timestamp, buffer, bufferSize);

		nextPacket.release();
		findNext();
		return videoMessage;
	}

	@Override
	public long seek(long timePosition) {
		container.seekKeyFrame(videoStreamId, timePosition, 0);
		findNext();
		if (nextPacket != null)
			return nextPacket.getTimeStamp();
		else
			return -1;
	}

	@Override
	public void setAggregateDuration(int targetDuration) {
		// TODO Auto-generated method stub
		
	}

}
