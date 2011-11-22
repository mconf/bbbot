package org.mconf.bbb.bot;

import org.mconf.bbb.BigBlueButtonClient;
import org.mconf.bbb.IBigBlueButtonClientListener;
import org.mconf.bbb.chat.ChatMessage;
import org.mconf.bbb.listeners.IListener;
import org.mconf.bbb.users.IParticipant;

import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IVideoPicture;


public class Bot extends BigBlueButtonClient implements IBigBlueButtonClientListener {
	
	String name = new String("BOT");
	String server = new String("http://mconf.org:8888");
	String serverSalt = new String("");
	String room = new String("");	
	String videoFileName = new String("");
	BotVideoPublish botVideoPublish;
	

	public String getServer() { return server;	}
	public String getRoom() { return room;	}
	public String getVideoFileName() { return videoFileName;	}
	
	public Bot(String server, String serverSalt, String room, String videoFileName){
		this.server = server;
		this.serverSalt = serverSalt;
		this.room = room;
		this.videoFileName = videoFileName;
	}
		
	public boolean connect(int number){
		name = name + Integer.toString(number);
		this.createJoinService(server, serverSalt);
		this.getJoinService().load();
		this.getJoinService().join(room, name, true);
		if (this.getJoinService().getJoinedMeeting() != null) {
			if(this.connectBigBlueButton())
				return true;
			else 
				return false;
		} else {
			System.out.println(name  + " failed to join the meeting");
			System.exit(1);
			return false;
		}
	}

	public void sendVideo() {
		IContainer container = IContainer.make();
			
		if(container.open(videoFileName, IContainer.Type.READ, null) < 0 ) {
			System.out.println("Sorry, could not read file.");
			System.exit(2);
		}
		
		int numStreams = container.getNumStreams();
		int videoStreamID = -1;
		IStreamCoder videoCoder = null;
		for(int i=0;i<numStreams;i++) {
			IStream stream = container.getStream(i);
			IStreamCoder coder = stream.getStreamCoder();
			
			System.out.println("Stream "+i+": "+coder.getCodecType());
			System.out.println("codec: " + coder.getCodecID());
						
			if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO) {
				videoStreamID = i;
				videoCoder = coder;
			}
		}
		if (videoStreamID == -1) {
			System.out.println("Sorry, there are no video streams on this file.");
			System.exit(3);	
		}
		if (videoCoder.open() < 0) {
			System.out.println("Sorry, failed to open video coder.");
			System.exit(4);
		}
		
		double frameRate = videoCoder.getFrameRate().getDouble();
		botVideoPublish = new BotVideoPublish(this, true, (int)frameRate , videoCoder.getWidth(), videoCoder.getHeight(), videoCoder.getBitRate()); 
		botVideoPublish.startPublisher();
		IPacket packet = IPacket.make();
		
		while(container.readNextPacket(packet) >= 0) {
			if (packet.getStreamIndex() == videoStreamID) {
				
				IVideoPicture picture = IVideoPicture.make(videoCoder.getPixelType(),
						videoCoder.getWidth(),
						videoCoder.getHeight()
						);
				
					
				int offset = 0;
			    while(offset < packet.getSize())
			    {
			          /*Now, we decode the video, checking for any errors.*/
			          int bytesDecoded = videoCoder.decodeVideo(picture, packet, offset);
			          if (bytesDecoded < 0)
			          {
			        	  System.out.println("bytes decoded < 0");
			        	  System.exit(51);
				      }
			          offset += bytesDecoded;
			          /*
			           * Some decoders will consume data in a packet, but will not be able to construct
			           * a full video picture yet.  Therefore you should always check if you
			           * got a complete picture from the decoder
			           */
			          if (picture.isComplete())
			          {
			        	  byte[] sharedBuffer = new byte[picture.getSize()];
			        	  sharedBuffer = picture.getData().getByteArray(0, picture.getSize()); //(offset, length)
			        	/*
			        	  System.out.println("sharedBuffer:");
						  System.out.println(sharedBuffer);
						  System.out.println("sharedBuffer lenght: " + sharedBuffer.length);
						
							for (byte _byte : sharedBuffer) {
								System.out.print(_byte + "+");
							}
						*/
			        	  long timeStamp =  picture.getTimeStamp();
						  this.botVideoPublish.onReadyFrame(sharedBuffer.length, (int)timeStamp, sharedBuffer);
			          }
			    }
			}
			else {
				//not a video packet
			}
	
		}
	
	
	}
	@Override
	public void onPublicChatMessage(ChatMessage message, IParticipant source) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onPrivateChatMessage(ChatMessage message, IParticipant source) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onConnected() {
		if (this.videoFileName != null)
			sendVideo();
	}
	@Override
	public void onDisconnected() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onKickUserCallback() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onParticipantLeft(IParticipant p) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onParticipantJoined(IParticipant p) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onParticipantStatusChangePresenter(IParticipant p) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onParticipantStatusChangeHasStream(IParticipant p) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onParticipantStatusChangeRaiseHand(IParticipant p) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onListenerJoined(IListener p) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onListenerLeft(IListener p) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onListenerStatusChangeIsMuted(IListener p) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onListenerStatusChangeIsTalking(IListener p) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onException(Throwable throwable) {
		// TODO Auto-generated method stub
	}
}
