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

import org.apache.commons.cli.UnrecognizedOptionException;
import org.mconf.bbb.BigBlueButtonClient;
import org.mconf.bbb.api.JoinServiceBase;
import org.mconf.bbb.api.Meeting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.xml.internal.ws.util.StringUtils;

public class BotManager {
	private static final Logger log = LoggerFactory.getLogger(BotManager.class);
		
	//Creates the Master (a BbbBot object), which will spawn the bots.
	public static void main (String[] args) throws IOException {
		BotManager Master = new BotManager(args);
		Master.spawnBots();
	}
	
	private int nBots = 1;
	private String server = "";
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
		
		if (options.has("s"))
			server = (String) options.valueOf("s");
		if (options.has("n"))
			nBots = Integer.parseInt((String) options.valueOf("n"));
		if (options.has("m"))
			room = (String) options.valueOf("m");
		if (options.has("p"))
			securityKey = (String) options.valueOf("p");
		if (options.has("meetings")) {
			BigBlueButtonClient client = new BigBlueButtonClient();
			client.createJoinService(server, securityKey);
			JoinServiceBase joinService = client.getJoinService();
			if (joinService == null) {
				log.error("Can't connect to the server, please check the server address");
				System.exit(1);
			}
			joinService.load();
			List<Meeting> meetings = client.getJoinService().getMeetings();
			if (meetings.isEmpty())
				log.info("No open meetings");
			else {
				log.info("Open meetings:");
				for (Meeting meeting : meetings) {
					log.info(meeting.toString());
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
		DecimalFormat format = new DecimalFormat(new String(new char[Integer.toString(nBots).length()]).replace("\0", "0"));
		for (int i = 1; i <= nBots; ++i) {
			Bot bot = new Bot(server, securityKey, room, videoFileName);
			botArmy.add(bot);
			bot.connect(format.format(i));
		}
	}

}
	