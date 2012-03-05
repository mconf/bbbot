//BigBlueButtonBot, GT-MCONF @PRAV-UFRGS, developed by Arthur C. Rauter, august 2011.
//Adding video to the bots through Xuggler library, September 2011.
package org.mconf.bbb.bot;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import joptsimple.HelpFormatter;
import joptsimple.OptionDescriptor;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.mconf.bbb.BigBlueButtonClient;
import org.mconf.bbb.api.JoinServiceBase;
import org.mconf.bbb.api.Meeting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BotManager {
	private static final Logger log = LoggerFactory.getLogger(BotManager.class);
		
	private int numbots = 1;
	private String server = "";
	private String securityKey = "";
	private String meeting = "";
	private List<Bot> botArmy = new ArrayList<Bot>();
	private String videoFilename = null;
	private String voiceFilename = null;
	private String name = "Bot";
	private String role = "MODERATOR";
	
	private boolean parse(String[] args) throws IOException {
		OptionParser parser = new OptionParser();
		parser.accepts("meetings", "displays the open rooms");
		parser.accepts("create", "creates a new meeting").withRequiredArg();
		parser.accepts("numbots", "number of bots").withRequiredArg().defaultsTo(Integer.toString(numbots));
		parser.accepts("meeting", "meeting id to spawn the bots").withRequiredArg();
		parser.accepts("server", "server address").withRequiredArg();
		parser.accepts("key", "server security key").withRequiredArg();
		parser.accepts("video", "video filename to be sent").withRequiredArg();
		parser.accepts("voice", "voice filename to be sent").withRequiredArg();
		parser.accepts("name", "name of the bots").withRequiredArg().defaultsTo(name);
		parser.accepts("role", "role of the bots in the conference (moderator|viewer)").withRequiredArg().defaultsTo(role);
		parser.accepts("help", "displays help information");
		
		HelpFormatter formatter = new HelpFormatter() {
			@Override
			public String format(Map<String, ? extends OptionDescriptor> options) {
	            StringBuilder buffer = new StringBuilder();
	            buffer.append("Developed for Mconf")
           		      .append(System.getProperty("line.separator"))
           		      .append("Bot commands:")
           		      .append(System.getProperty("line.separator"));
	            for ( OptionDescriptor each : new HashSet<OptionDescriptor>( options.values() ) ) {
	                buffer.append( lineFor( each ) );
	            }
	            return buffer.toString();
			}

	        private String lineFor( OptionDescriptor descriptor ) {
	            StringBuilder line = new StringBuilder( descriptor.options().toString() );
	            line.append( ": " ).append( descriptor.description() );
	            if (!descriptor.defaultValues().isEmpty())
	            	line.append( " " ).append(descriptor.defaultValues() );
	            line.append( System.getProperty( "line.separator" ) );
	            return line.toString();
	        }
		};
		parser.formatHelpWith(formatter);
		
		OptionSet options = parser.parse(args);
		
		if (options.has("help")) {
			parser.printHelpOn( System.out );
			return false;
		}
		if (options.has("server"))
			server = (String) options.valueOf("server");
		if (options.has("numbots"))
			numbots = Integer.parseInt((String) options.valueOf("numbots"));
		if (options.has("meeting"))
			meeting = (String) options.valueOf("meeting");
		if (options.has("key"))
			securityKey = (String) options.valueOf("key");
		if (options.has("meetings")) {
			return printOpenMeetings();
		}
		if (options.has("create")) {
			BigBlueButtonClient client = new BigBlueButtonClient();
			client.createJoinService(server, securityKey);
			JoinServiceBase joinService = client.getJoinService();
			if (joinService == null) {
				log.error("Can't connect to the server, please check the server address");
				return false;
			}
			joinService.createMeeting((String) options.valueOf("create"));
			return printOpenMeetings();
		}
		if (options.has("video"))
			videoFilename = (String) options.valueOf("video");
		if (options.has("voice"))
			voiceFilename = (String) options.valueOf("voice");
		if (options.has("name"))
			name = (String) options.valueOf("name");
		if (options.has("role")) {
			role = ((String) options.valueOf("role")).toUpperCase();
			if (!role.equals("MODERATOR") && !role.equals("VIEWER")) {
				log.error("Invalid role selected: {}", role);
				return false;
			}
		}
		return true;
	}
	
	private boolean printOpenMeetings() {
		BigBlueButtonClient client = new BigBlueButtonClient();
		client.createJoinService(server, securityKey);
		JoinServiceBase joinService = client.getJoinService();
		if (joinService == null) {
			log.error("Can't connect to the server, please check the server address");
			return false;
		}
		if (!joinService.load())
			return false;
		List<Meeting> meetings = client.getJoinService().getMeetings();
		if (meetings.isEmpty())
			log.info("No open meetings");
		else {
			log.info("Open meetings:");
			for (Meeting meeting : meetings) {
				log.info(meeting.toString());
			}
		}
		return false;
	}

	public void spawnBots() throws InterruptedException {
		DecimalFormat format = new DecimalFormat(new String(new char[Integer.toString(numbots).length()]).replace("\0", "0"));
		for (int i = 1; i <= numbots; ++i) {
			Bot bot = new Bot();
			botArmy.add(bot);
			log.info("CONECTING {}", name + " " + format.format(i));
			bot.connect(server, securityKey, meeting, name + " " + format.format(i),
					    role.equals("MODERATOR"), videoFilename, voiceFilename);
			Thread.sleep(5000);
		}
	}

	public static void main (String[] args) throws IOException, InterruptedException {
		BotManager master = new BotManager();
		if (master.parse(args))
			master.spawnBots();
	}
		
}
	