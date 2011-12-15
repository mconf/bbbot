package org.mconf.bbb.bot;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mconf.bbb.BigBlueButtonClient;
import org.mconf.bbb.BigBlueButtonClient.OnAudioListener;
import org.mconf.bbb.BigBlueButtonClient.OnConnectedListener;
import org.mconf.bbb.BigBlueButtonClient.OnParticipantJoinedListener;
import org.mconf.bbb.BigBlueButtonClient.OnParticipantLeftListener;
import org.mconf.bbb.BigBlueButtonClient.OnParticipantStatusChangeListener;
import org.mconf.bbb.BigBlueButtonClient.OnPublicChatMessageListener;
import org.mconf.bbb.api.JoinService0Dot8;
import org.mconf.bbb.api.JoinServiceBase;
import org.mconf.bbb.chat.ChatMessage;
import org.mconf.bbb.users.IParticipant;
import org.mconf.bbb.users.Participant;
import org.mconf.bbb.video.BbbVideoPublisher;
import org.mconf.bbb.video.BbbVideoReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.flazr.io.flv.FlvReader;
import com.flazr.rtmp.RtmpReader;
import com.flazr.rtmp.message.Audio;
import com.flazr.rtmp.message.Video;

public class Bot extends BigBlueButtonClient implements 
		OnParticipantJoinedListener, 
		OnParticipantLeftListener, 
		OnParticipantStatusChangeListener, 
		OnConnectedListener, 
		OnAudioListener,
		OnPublicChatMessageListener
{

	private static final Logger log = LoggerFactory.getLogger(Bot.class);
	
	private String videoFilename;

	private Map<Integer, BbbVideoReceiver> remoteVideos = new HashMap<Integer, BbbVideoReceiver>();

	public boolean connect(String server, String securityKey, String meeting, String name, boolean moderator, String videoFilename) {
		this.videoFilename = videoFilename;
		
		createJoinService(server, securityKey);
		JoinServiceBase joinService = getJoinService();
		if (joinService == null) {
			log.error("Can't connect to the server, please check the server address");
			return false;
		}
		if (!joinService.load()) {
			return false;
		}
		joinService.join(meeting, name, moderator);
		if (joinService.getJoinedMeeting() != null) {
			addParticipantJoinedListener(this);
			addParticipantStatusChangeListener(this);
			addConnectedListener(this);
			addAudioListener(this);
			addPublicChatMessageListener(this);
			return (connectBigBlueButton());
		} else {
			log.error(name  + " failed to join the meeting");
			System.exit(1);
			return false;
		}
	}
	
	public void sendVideo() {
		RtmpReader reader = null;
		try {
			reader = new XugglerFlvReader(videoFilename);
		} catch (Throwable e) {
			log.error("You don't have Xuggler installed");
		}
		
		if (reader == null) {
			try {
				reader = new FlvReader(videoFilename);
			} catch (Exception e) {
				log.error("Can't create a FlvReader instance");
			}
		}
		
		if (reader != null) {
	    	String streamName = reader.getWidth() + "x" + reader.getHeight() + getMyUserId();
	    	if (getJoinService().getClass() == JoinService0Dot8.class)
	    		streamName += "-" + new Date().getTime();
			
			BbbVideoPublisher publisher = new BbbVideoPublisher(this, reader, streamName);
			publisher.setLoop(true);
			publisher.start();
		}
	}
	
	@Override
	public void onParticipantJoined(IParticipant p) {
		if (p.getUserId() == getMyUserId()) { 
			if (videoFilename != null && videoFilename.length() > 0)
				sendVideo();
		} else {
			if (p.getStatus().isHasStream())
				startReceivingVideo(p.getUserId());
		}
	}

	@Override
	public void onParticipantLeft(IParticipant p) {
		if (p.getStatus().isHasStream())
			stopReceivingVideo(p.getUserId());
	}

	@Override
	public void onChangePresenter(IParticipant p) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onChangeHasStream(IParticipant p) {
		if (p.getUserId() == getMyUserId())
			return;
		
		if (p.getStatus().isHasStream()) {
			startReceivingVideo(p.getUserId());
		} else {
			stopReceivingVideo(p.getUserId());
		}
	}
	
	private void startReceivingVideo(int userId) {
		BbbVideoReceiver videoReceiver = new BbbVideoReceiver(userId, this) {
			
			@Override
			protected void onVideo(Video video) {
				log.debug("received video package: {}", video.getHeader().getTime());
			}
		};
		remoteVideos.put(userId, videoReceiver);
		videoReceiver.start();
	}
	
	private void stopReceivingVideo(int userId) {
		BbbVideoReceiver videoReceiver = remoteVideos.get(userId);
		if (videoReceiver != null) {
			videoReceiver.stop();
			remoteVideos.remove(userId);
		}
	}

	@Override
	public void onChangeRaiseHand(IParticipant p) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onConnectedSuccessfully() {
		connectVoice();
	}

	@Override
	public void onConnectedUnsuccessfully() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAudio(Audio audio) {
		log.debug("received audio package: {}", audio.getHeader().getTime());
	}

	@Override
	public void onPublicChatMessage(ChatMessage message, IParticipant source) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPublicChatMessage(List<ChatMessage> publicChatMessages,
			Map<Integer, Participant> participants) {
		sendPublicChatMessage("Hi, this is a test");
	}
}
