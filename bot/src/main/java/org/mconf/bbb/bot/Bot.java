package org.mconf.bbb.bot;

import java.text.DecimalFormat;
import java.util.Date;

import org.mconf.bbb.BigBlueButtonClient;
import org.mconf.bbb.BigBlueButtonClient.OnParticipantJoinedListener;
import org.mconf.bbb.api.JoinService0Dot8;
import org.mconf.bbb.api.JoinServiceBase;
import org.mconf.bbb.users.IParticipant;
import org.mconf.bbb.video.BbbVideoPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.flazr.rtmp.server.ServerStream.PublishType;


public class Bot extends BigBlueButtonClient implements OnParticipantJoinedListener {

	private static final Logger log = LoggerFactory.getLogger(BotManager.class);
	
	private String name = "Bot";
	private String server = "";
	private String serverSalt = "";
	private String room = "";	
	private String videoFileName = "";

	public String getServer() { return server;	}
	public String getRoom() { return room;	}
	public String getVideoFileName() { return videoFileName;	}
	
	public Bot(String server, String serverSalt, String room, String videoFileName){
		this.server = server;
		this.serverSalt = serverSalt;
		this.room = room;
		this.videoFileName = videoFileName;
	}
		
	public boolean connect(String number) {
		name = name + " " + number;
		createJoinService(server, serverSalt);
		JoinServiceBase joinService = getJoinService();
		if (joinService == null) {
			log.error("Can't connect to the server, please check the server address");
			return false;
		}
		joinService.load();
		joinService.join(room, name, true);
		if (joinService.getJoinedMeeting() != null) {
			addParticipantJoinedListener(this);
			if (connectBigBlueButton())
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
		XugglerFlvReader reader = null;
		try {
			reader = new XugglerFlvReader(this.videoFileName);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
    	String streamName = reader.getWidth() + "x" + reader.getHeight() + getMyUserId();
    	if (getJoinService().getClass() == JoinService0Dot8.class)
    		streamName += "-" + new Date().getTime();
		
		BbbVideoPublisher publisher = new BbbVideoPublisher(this, reader, streamName);
		publisher.setLoop(true);
		publisher.setPublishType(PublishType.RECORD);
		publisher.start();
	}
	@Override
	public void onParticipantJoined(IParticipant p) {
		if (p.getUserId() == getMyUserId() && videoFileName != null && videoFileName.length() > 0)
			sendVideo();
	}
}
