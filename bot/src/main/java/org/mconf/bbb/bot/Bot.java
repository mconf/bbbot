package org.mconf.bbb.bot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mconf.bbb.BigBlueButtonClient;
import org.mconf.bbb.BigBlueButtonClient.OnConnectedListener;
import org.mconf.bbb.BigBlueButtonClient.OnParticipantJoinedListener;
import org.mconf.bbb.BigBlueButtonClient.OnParticipantLeftListener;
import org.mconf.bbb.BigBlueButtonClient.OnParticipantStatusChangeListener;
import org.mconf.bbb.BigBlueButtonClient.OnPublicChatMessageListener;
import org.mconf.bbb.api.JoinService0Dot8;
import org.mconf.bbb.api.JoinServiceBase;
import org.mconf.bbb.chat.ChatMessage;
import org.mconf.bbb.phone.BbbVoiceConnection;
import org.mconf.bbb.users.IParticipant;
import org.mconf.bbb.users.Participant;
import org.mconf.bbb.video.BbbVideoPublisher;
import org.mconf.bbb.video.BbbVideoReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.flazr.io.flv.FlvReader;
import com.flazr.rtmp.RtmpReader;
import com.flazr.rtmp.message.Video;

public class Bot extends BigBlueButtonClient implements 
		OnParticipantJoinedListener, 
		OnParticipantLeftListener, 
		OnParticipantStatusChangeListener, 
		OnConnectedListener, 
		OnPublicChatMessageListener
{

	private static final Logger log = LoggerFactory.getLogger(Bot.class);
	
	public BbbVoiceConnection voiceConnection;

	private Map<Integer, BbbVideoReceiver> remoteVideos = new HashMap<Integer, BbbVideoReceiver>();

	private String server;
	private String securityKey;
	private String meetingId;
	private String name;
	private boolean moderator;
	private String videoFilename;
	private String audioFilename;
	private boolean sendVideo;
	private boolean recvVideo;
	private boolean sendAudio;
	private boolean recvAudio;

	private boolean create;

	private RtmpReader videoReader;
	
	private void sendVideo() {
		RtmpReader reader = videoReader;
//		RtmpReader reader = null;

//		try {
//			reader = new FlvReader(videoFilename);
//		} catch (Exception e) {
//			log.error("Can't create a FlvReader instance for " + videoFilename);
//		}
		
		if (reader != null) {
	    	String streamName = reader.getWidth() + "x" + reader.getHeight() + getMyUserId();
	    	if (getJoinService().getClass() == JoinService0Dot8.class)
	    		streamName += "-" + new Date().getTime();
			
			BbbVideoPublisher publisher = new BbbVideoPublisher(this, reader, streamName);
			publisher.setLoop(true);
			publisher.start();
		}
	}
	
	private void connectVoice() {
		RtmpReader reader = null;
		if (audioFilename != null && audioFilename.length() > 0 && sendAudio) {
			try {
				reader = new FlvReader(audioFilename);
			} catch (Exception e) {
				e.printStackTrace();
				log.error("Can't create a FlvReader instance for " + audioFilename);
			}
		}

		voiceConnection = new BbbVoiceConnection(this, reader);
		voiceConnection.setLoop(true);
		voiceConnection.start();
	}

	@Override
	public void onParticipantJoined(IParticipant p) {
		if (p.getUserId() == getMyUserId()) { 
			if (videoFilename != null && videoFilename.length() > 0 && sendVideo)
				sendVideo();
		} else {
			if (p.getStatus().isHasStream() && recvVideo)
				startReceivingVideo(p.getUserId());
		}
	}

	@Override
	public void onParticipantLeft(IParticipant p) {
		if (p.getStatus().isHasStream() && recvVideo)
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
		
		if (recvVideo) {
			if (p.getStatus().isHasStream()) {
				startReceivingVideo(p.getUserId());
			} else {
				stopReceivingVideo(p.getUserId());
			}
		}
	}
	
	private void startReceivingVideo(int userId) {
		BbbVideoReceiver videoReceiver = new BbbVideoReceiver(userId, this) {
			
			@Override
			protected void onVideo(Video video) {
				// it will log a message when it receives a video packet
				super.onVideo(video);
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
		if (recvAudio || sendAudio)
			connectVoice();
	}

	@Override
	public void onConnectedUnsuccessfully() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPublicChatMessage(ChatMessage message, IParticipant source) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPublicChatMessage(List<ChatMessage> publicChatMessages,
			Map<Integer, Participant> participants) {
		sendPublicChatMessage("Logged in on " + new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(Calendar.getInstance().getTime()).toString());
	}

	public void setServer(String server) {
		this.server = server;
	}

	public void setSecurityKey(String securityKey) {
		this.securityKey = securityKey;
	}

	public void setMeetingId(String meetingId) {
		this.meetingId = meetingId;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setRole(String role) {
		this.moderator = role.equals("moderator");
	}

	public void setVideoFilename(String videoFilename) {
		this.videoFilename = videoFilename;
	}

	public void setAudioFilename(String audioFilename) {
		this.audioFilename = audioFilename;
	}

	public void setSendVideo(boolean sendVideo) {
		this.sendVideo = sendVideo;
	}

	public void setReceiveVideo(boolean recvVideo) {
		this.recvVideo = recvVideo;
	}

	public void setSendAudio(boolean sendAudio) {
		this.sendAudio = sendAudio;
	}

	public void setReceiveAudio(boolean recvAudio) {
		this.recvAudio = recvAudio;
	}

	public void start() {
		createJoinService(server, securityKey);
		JoinServiceBase joinService = getJoinService();
		if (joinService == null) {
			log.error("Can't connect to the server, please check the server address");
			return;
		}
		if (create && joinService.createMeeting(meetingId) != JoinServiceBase.E_OK) {
			return;
		}
		if (joinService.load() != JoinServiceBase.E_OK) {
			return;
		}
		if (joinService.join(meetingId, name, moderator) != JoinServiceBase.E_OK) {
			return;
		}
		if (joinService.getJoinedMeeting() != null) {
			addParticipantJoinedListener(this);
			addParticipantStatusChangeListener(this);
			addConnectedListener(this);
			addPublicChatMessageListener(this);
			connectBigBlueButton();
		} else {
			log.error(name  + " failed to join the meeting");
		}
	}

	public void setCreateMeeting(boolean create) {
		this.create = create;
	}

	public void setVideoReader(RtmpReader reader) {
		this.videoReader = reader;
		
	}
}
