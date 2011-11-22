package org.mconf.bbb.bot;
import java.util.ArrayList;
import java.util.List;

import org.mconf.bbb.BigBlueButtonClient;
import org.mconf.bbb.video.IVideoPublishListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.flazr.rtmp.RtmpReader;
import com.flazr.rtmp.message.Metadata;
import com.flazr.rtmp.message.Video;

public class BotVideoPublish extends Thread implements RtmpReader {
	private class VideoPublishHandler extends IVideoPublishListener {
		
		public VideoPublishHandler(int userId, String streamName, RtmpReader reader, BigBlueButtonClient context) {			
			super(userId, streamName, reader, context);
		}
				
	}
	
	private static final Logger log = LoggerFactory.getLogger(BotVideoPublish.class);
	
	public int frameRate;
    public int width;
    public int height;
    public int bitRate;
    public int GOP;
      	
	private List<Video> framesList = new ArrayList<Video>();
	
	private VideoPublishHandler videoPublishHandler;
	
	private BigBlueButtonClient context;
	
	//private byte[] sharedBuffer;
	
	public int bufSize;
	
	private int firstTimeStamp = 0;
	private int lastTimeStamp = 0;
	    
    public int state;
        
    public boolean nextSurfaceCreated = false; // used when:
    										// the activity or the orientation changes and 
    										// the video was being captured (except if 
	   // we are faking a destruction - see the VideoCapture.fakeDestroyed variable for more info). 
    										// In this moment,
    										// there are 2 surfaces conflicting, and we need to know
    										// if/when they are destroyed and created.
    										// true when: the next surface has already been created
    										// false when: the next surface has not been created yet OR 
    										//             there isn't a 2 surfaces conflict
    public boolean lastSurfaceDestroyed = false; // used when:
    										// same situation as the "nextSurfaceCreated" variable
    										// true when: the last preview surface has already been destroyed
    										// false when: the last preview surface is still active
    
    public boolean nativeEncoderInitialized = false; // used to prevent errors.
    												 // true when the native class VideoEncoder is not NULL
    												 // false when the native class VideoEncoder is NULL
    
    public boolean restartWhenResume; // used in the following situation:
    								  // the user put the application in background.
    								  // now the user put the application in foreground again.
    								  // in this situation, this boolean is true if the camera was being 
    								  // captured when the application went to background, and false if the
    								  // camera was not being captured.
    								  // So, this boolean allows to keep the previous state (capturing or not)
    								  // when the application resumes.
    
    private boolean framesListAvailable = false; // set to true when the RtmpPublisher starts seeking
    										     // for video messages. When true, this boolean allows the addition
    											 // of video frames to the list.
    											 // Set to false right when the RtmpPublisher decides to 
    											 // close the reader. When false, this boolean prevents the
    											 // addition of new frames to the list.
    
    private boolean firstFrameWrote = false;
	        
    public BotVideoPublish(BigBlueButtonClient context, boolean restartWhenResume, int frameRate, int width, int height, int bitRate) {
    	this.context = context;    	 
    	this.restartWhenResume = restartWhenResume;
    	this.frameRate = frameRate;
    	this.width = width;
    	this.height = height;
    	this.bitRate = bitRate;
    	
    }
    
    public void startPublisher(){
    	videoPublishHandler = new VideoPublishHandler(context.getMyUserId(), width+"x"+height+context.getMyUserId(), this, context);
    	videoPublishHandler.start();
    }        	
    
    public void stopPublisher(){
    	synchronized(this) {
			this.notifyAll();
		}
    	if(videoPublishHandler != null){
    		videoPublishHandler.stop(context);
    	}
    }
        
    public void endNativeEncoder(){
    	nativeEncoderInitialized = false;
        	
    	endEncoder();
    }
    
    @Override
    public void run() {       	
    	initSenderLoop();
    }
        
    public int onReadyFrame (int bufferSize, int timeStamp, byte[] sharedBuffer)
    {    	
    	
    	if(firstTimeStamp == 0){
    		firstTimeStamp = timeStamp;
    	}    	
    	timeStamp = timeStamp - firstTimeStamp;
    	int interval = timeStamp - lastTimeStamp;
    	lastTimeStamp = timeStamp;
    	
    	byte[] aux = new byte[bufferSize];
    	System.arraycopy(sharedBuffer, 0, aux, 0, bufferSize);
    	
       	Video video = new Video(timeStamp, aux, bufferSize);
   	    video.getHeader().setDeltaTime(interval);
		video.getHeader().setStreamId(videoPublishHandler.videoConnection.streamId);
		
		if(context.getUsersModule().getParticipants().get(context.getMyUserId()).getStatus().isHasStream()
		   && framesListAvailable && framesList != null)
		{
			
			framesList.add(video);
//			System.out.println("fired");
			if(true)//!firstFrameWrote)
			{
				if(videoPublishHandler != null 
						&& videoPublishHandler.videoConnection != null
						&& videoPublishHandler.videoConnection.publisher != null
						&& videoPublishHandler.videoConnection.publisher.isStarted()) 
				{
					firstFrameWrote = true;
					videoPublishHandler.videoConnection.publisher.fireNext(videoPublishHandler.videoConnection.publisher.channel, 0);
				
				} 
				else {
					log.debug("Warning: tried to fireNext but video publisher is not started");
				}
			}
			//synchronized(this) {
			//	this.notifyAll();
			//}
		}
		
    	return 0;
    }

	@Override
	public void close() {		
		framesListAvailable = false;
		if(framesList != null){
			framesList.clear();
		}
		framesList = null;
	}

	@Override
	public Metadata getMetadata() {
		return null;
	}

	@Override
	public Video[] getStartMessages() {
		framesListAvailable = true;
		Video[] startMessages = new Video[0];
        return startMessages;
	}

	@Override
	public long getTimePosition() {
		return 0;
	}

	@Override
	public boolean hasNext() {
		if(framesListAvailable && framesList != null && framesList.isEmpty()){
			try {
				this.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		if(framesListAvailable && framesList != null){ // means that the framesList is not empty
			return true;
		} else { // means that the framesList is empty or we should not get next frames
			return false;
		}
	}

	@Override
	public Video next() {
		if(framesListAvailable && framesList != null){
			return framesList.remove(0);
		} else {
			Video emptyVideo = new Video();
	        return emptyVideo;
		}
	}

	@Override
	public long seek(long timePosition) {
		return 0;
	}

	@Override
	public void setAggregateDuration(int targetDuration) {
	}
	
	private native int initEncoder(int width, int height, int frameRate, int bitRate, int GOP);
	private native int endEncoder();
    private native int initSenderLoop();
}