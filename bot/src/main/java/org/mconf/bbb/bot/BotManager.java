//BigBlueButtonBot, GT-MCONF @PRAV-UFRGS, developed by Arthur C. Rauter, august 2011.
//Adding video to the bots through Xuggler library, September 2011.
package org.mconf.bbb.bot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import joptsimple.HelpFormatter;
import joptsimple.OptionDescriptor;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.mconf.bbb.BigBlueButtonClient;
import org.mconf.bbb.api.Meeting;

public class BotManager {
		
	//Creates the Master (a BbbBot object), which will spawn the bots.
	public static void main (String[] args) throws IOException {
		BotManager Master = new BotManager(args);
		Master.spawnBots();
//		if (Master.getVideoFileName().equals("")) {
//			//do nothing
//		}
//		else {
//			//video enabled
//			Master.sendBotsVideo();
//		}
		
	}
	
	private int nBots = 1;
	private String server = "http://mconf.org:8888";
	private String securityKey = "";
	private String room = "";
	private List<Bot> botArmy = new ArrayList<Bot>();
	private String videoFileName = null;
	
	//processes the arguments
	public BotManager(String[] args) throws IOException{
		OptionParser parser = new OptionParser();
		parser.accepts("meetings", "open rooms");
		parser.accepts("n", "number of bots").withRequiredArg().defaultsTo(Integer.toString(nBots));
		parser.accepts("m", "meeting id to spawn the bots").withRequiredArg();
		parser.accepts("s", "server address").withRequiredArg().defaultsTo(server);
		parser.accepts("p", "password").withRequiredArg();
		parser.accepts("v", "video filename to be sent").withRequiredArg();
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
//	            line.append( ": description = " ).append( descriptor.description() );
//	            line.append( ", required = " ).append( descriptor.isRequired() );
//	            line.append( ", accepts arguments = " ).append( descriptor.acceptsArguments() );
//	            line.append( ", requires argument = " ).append( descriptor.requiresArgument() );
//	            line.append( ", argument description = " ).append( descriptor.argumentDescription() );
//	            line.append( ", argument type indicator = " ).append( descriptor.argumentTypeIndicator() );
//	            line.append( ", default values = " ).append( descriptor.defaultValues() );
	            line.append( System.getProperty( "line.separator" ) );
	            return line.toString();
	        }
		};
		parser.formatHelpWith(formatter);
		
		OptionSet options = parser.parse(args);
		
		if (options.has("n"))
			nBots = (Integer) options.valueOf("n");
		if (options.has("m"))
			room = (String) options.valueOf("m");
		if (options.has("s"))
			server = (String) options.valueOf("s");
		if (options.has("p"))
			securityKey = (String) options.valueOf("p");
		if (options.has("meetings")) {
			BigBlueButtonClient client = new BigBlueButtonClient();
			client.createJoinService(server, securityKey);
			client.getJoinService().load();
			List<Meeting> meetings = client.getJoinService().getMeetings();
			if (meetings.isEmpty())
				System.out.println("No open meetings");
			else {
				System.out.println("Open meetings:");
				for (Meeting meeting : meetings) {
					System.out.println(meeting);
				}
			}
			System.exit(0);
		}
		if (options.has("v"))
			videoFileName = (String) options.valueOf("v");
		if (options.has("help")) {
			parser.printHelpOn( System.out );
			System.exit(0);
		}
	}
		
	public void spawnBots() {
		for (int i = 0; i < nBots; ++i) {
			Bot bot = new Bot(server, securityKey, room, videoFileName);
			botArmy.add(bot);
			bot.connect(i);
		}
	}

}
	