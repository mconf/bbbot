package org.mconf.bbb.bot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.mconf.bbb.BigBlueButtonClient;
import org.mconf.bbb.BigBlueButtonClient.OnConnectedListener;
import org.mconf.bbb.BigBlueButtonClient.OnDisconnectedListener;
import org.mconf.bbb.BigBlueButtonClient.OnParticipantJoinedListener;
import org.mconf.bbb.BigBlueButtonClient.OnParticipantLeftListener;
import org.mconf.bbb.BigBlueButtonClient.OnParticipantStatusChangeListener;
import org.mconf.bbb.BigBlueButtonClient.OnPublicChatMessageListener;
import org.mconf.bbb.api.JoinService0Dot8;
import org.mconf.bbb.api.JoinService0Dot81;
import org.mconf.bbb.api.JoinService0Dot9;
import org.mconf.bbb.api.JoinServiceBase;
import org.mconf.bbb.chat.ChatMessage;
import org.mconf.bbb.deskshare.BbbDeskshareConnection;
import org.mconf.bbb.phone.BbbVoiceConnection;
import org.mconf.bbb.users.IParticipant;
import org.mconf.bbb.users.Participant;
import org.mconf.bbb.video.BbbVideoPublisher;
import org.mconf.bbb.video.BbbVideoReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.flazr.io.flv.FlvReader;
import com.flazr.rtmp.RtmpReader;
import com.flazr.rtmp.RtmpWriter;
import com.flazr.rtmp.message.Video;

public class Bot extends BigBlueButtonClient implements 
		OnParticipantJoinedListener, 
		OnParticipantLeftListener, 
		OnParticipantStatusChangeListener, 
		OnConnectedListener, 
		OnDisconnectedListener,
		OnPublicChatMessageListener
{

	private static final Logger log = LoggerFactory.getLogger(Bot.class);
	
	public BbbVoiceConnection voiceConnection;
	public BbbDeskshareConnection deskshareConnection;

	private Map<String, BbbVideoReceiver> remoteVideos = new HashMap<String, BbbVideoReceiver>();

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
	private boolean recvDeskshare;

	private boolean create;

	private FlvPreLoader videoLoader;
	private BbbVideoPublisher videoPublisher;
	
	private boolean disconnected_myself = false;
	private boolean chat_sent = false;

	private boolean recordAudio;
	private int audioSampleSize;
	private int numberOfAudioSamples;
	private boolean listenOnly;
	
	private void sendVideo() {
		RtmpReader reader = new GlobalFlvReader(videoLoader);
		String streamName = reader.getWidth() + "x" + reader.getHeight() + getMyUserId();
		if (getJoinService().getClass() == JoinService0Dot8.class) {
			streamName += "-" + new Date().getTime();
		} else if (getJoinService().getClass() == JoinService0Dot81.class ||
				getJoinService().getClass() == JoinService0Dot9.class) {
			streamName = "low-" + getMyUserId() + "-" + new Date().getTime();
		}
		
		videoPublisher = new BbbVideoPublisher(this, reader, streamName);
		videoPublisher.setLoop(true);
		videoPublisher.start();
	}

	private void connectDeskshare() {
		deskshareConnection = new BbbDeskshareConnection(this) {
			/* TODO: We must review this
			 * When we reconnect we change the RTMP channel and
			 * this should be refreshed also at the deskshare module
			 */
			private void reconnect() {
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					// do nothing, just return
					return;
				}
				deskshareConnection.start();
			}

			@Override
			public void channelDisconnected(ChannelHandlerContext ctx,
					ChannelStateEvent e) throws Exception {
				super.channelDisconnected(ctx, e);
				if (!disconnected_myself) {
					log.error("{} has dropped from the deskshare conference, reconnecting to {}", name, getJoinService().getApplicationService().getServerUrl());
					reconnect();
				}
			}

			@Override
			protected void onConnectedUnsuccessfully() {
				if (!disconnected_myself) {
					log.error("The deskshare connection of {} to {} was unsucceeded, trying one more time", name, getJoinService().getApplicationService().getServerUrl());
					reconnect();
				}
			}
		};

		deskshareConnection.start();
	}
	
	private void connectVoice() {
		
		RtmpReader reader = null;
		RtmpWriter writer = null;
		if (audioFilename != null && audioFilename.length() > 0) {
			if (sendAudio) {
				try {
					reader = new FlvReader(audioFilename);
				} catch (Exception e) {
					e.printStackTrace();
					log.error("Can't create a FlvReader instance for " + audioFilename);
				}
			} else if (recordAudio) {
				try {
					writer = new LimitedSizeFlvWriter(audioFilename, audioSampleSize);
				} catch (Exception e) {
					e.printStackTrace();
					log.error("Can't create a FlvWriter instance for " + audioFilename);
				}
			}
		}

		voiceConnection = new BbbVoiceConnection(this, reader, writer) {
			private void reconnect() {
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					// do nothing, just return
					return;
				}
				voiceConnection.start();
			}
			
			@Override
			public void channelDisconnected(ChannelHandlerContext ctx,
					ChannelStateEvent e) throws Exception {
				super.channelDisconnected(ctx, e);
				if (!disconnected_myself) {
					log.error("{} has dropped from the voice conference, reconnecting to {}", name, getJoinService().getApplicationService().getServerUrl());
					reconnect();
				}
			}
			
			@Override
			protected void onConnectedUnsuccessfully() {
				if (!disconnected_myself) {
					log.error("The voice connection of {} to {} was unsucceeded, trying one more time", name, getJoinService().getApplicationService().getServerUrl());
					reconnect();
				}
			}
		};
		voiceConnection.setListenOnly(listenOnly);
		voiceConnection.setLoop(true);
		voiceConnection.start();
	}

	@Override
	public void onParticipantJoined(IParticipant p) {
		if (isMyself(p.getUserId())) { 
			if (recvAudio || sendAudio) {
				connectVoice();
			}
			if (videoFilename != null && videoFilename.length() > 0 && sendVideo) {
				sendVideo();
			}
			if (recvDeskshare) {
				connectDeskshare();
			}
		} else {
			if (p.getStatus().doesHaveStream() && recvVideo)
				startReceivingVideo(p.getUserId());
		}
	}

	@Override
	public void onParticipantLeft(IParticipant p) {
		if (p.getStatus().doesHaveStream() && recvVideo)
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
			if (p.getStatus().doesHaveStream()) {
				startReceivingVideo(p.getUserId());
			} else {
				stopReceivingVideo(p.getUserId());
			}
		}
	}
	
	private void startReceivingVideo(String userId) {
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
	
	private void stopReceivingVideo(String userId) {
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
	}

	@Override
	public void onConnectedUnsuccessfully() {
		log.error("The connection of {} to {} was unsucceeded", name, getJoinService().getApplicationService().getServerUrl());
	}

	@Override
	public void onPublicChatMessage(ChatMessage message, IParticipant source) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPublicChatMessage(List<ChatMessage> publicChatMessages,
			Map<String, Participant> participants) {
		if (!chat_sent) {
			chat_sent = true;
			sendPublicChatMessage("Logged in on " + new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(Calendar.getInstance().getTime()).toString());
		}
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

	public void setReceiveDeskshare(boolean recvDeskshare) {
		this.recvDeskshare = recvDeskshare;
	}

	public void start() {
		disconnected_myself = false;
		createJoinService(server, securityKey);
		JoinServiceBase joinService = getJoinService();
		if (joinService == null) {
			log.error("Can't connect to the server, please check the server address");
			return;
		}
		if (create && joinService.createMeeting(meetingId) != JoinServiceBase.E_OK) {
			log.error("Can't create the room {}, but I will continue on my task", meetingId);
		}
		if (joinService.load() != JoinServiceBase.E_OK) {
			log.error("Can't load the join service");
			return;
		}
		if (joinService.join(meetingId, name, moderator) != JoinServiceBase.E_OK) {
			log.error("Can't join the room {}", meetingId);
			return;
		}
		if (joinService.getJoinedMeeting() != null) {
			addParticipantJoinedListener(this);
			addParticipantStatusChangeListener(this);
			addConnectedListener(this);
			addDisconnectedListener(this);
			addPublicChatMessageListener(this);
			if (!connectBigBlueButton()) {
				log.error("Failed to connect to BigBlueButton");
				return;
			}
		} else {
			log.error(name  + " failed to join the meeting");
			return;
		}

	}

	private void stop() {
		removeParticipantJoinedListener(this);
		removeParticipantStatusChangeListener(this);
		removeConnectedListener(this);
		removeDisconnectedListener(this);
		removePublicChatMessageListener(this);

		Object[] receiver = remoteVideos.values().toArray();
		for (int i = receiver.length; i > 0; i--)
			 ((BbbVideoReceiver) receiver[i - 1]).stop();
		remoteVideos.clear();
		
		if (videoPublisher != null)
			videoPublisher.stop();
		if (voiceConnection != null)
			voiceConnection.stop();
		if (deskshareConnection != null)
			deskshareConnection.stop();

		// TODO: This wait is needed to avoid some race condition. Could be revised later
		try {
			Thread.sleep(25);
		} catch(InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	public void setCreateMeeting(boolean create) {
		this.create = create;
	}

	public void setVideoLoader(FlvPreLoader loader) {
		this.videoLoader = loader;
	}

	@Override
	public void disconnect() {
		disconnected_myself = true;
		stop();
		super.disconnect();
	}

	@Override
	public void onDisconnected() {
		if (!disconnected_myself) {
			log.error("{} has been disconnected, reconnecting", name);
			stop();
			start();
		}
	}

	public void setRecordAudio(boolean record_audio) {
		this.recordAudio = record_audio; 
	}

	public void setAudioSampleSize(int audio_sample_size) {
		this.audioSampleSize = audio_sample_size;
	}

	public void setNumberOfAudioSamples(int number_of_audio_samples) {
		this.numberOfAudioSamples = number_of_audio_samples;
	}

	public void setListenOnly(boolean listen_only) {
		this.listenOnly = listen_only;
	}
}
