//BigBlueButtonBot, GT-MCONF @PRAV-UFRGS, developed by Arthur C. Rauter, august 2011.
//Adding video to the bots through Xuggler library, September 2011.
package org.mconf.bbb.bot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.mconf.bbb.BigBlueButtonClient;
import org.mconf.bbb.api.JoinServiceBase;
import org.mconf.bbb.api.Meeting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import org.mconf.bbb.bot.ProbabilitiesConverter;

public class BotLauncher {
	private static final Logger log = LoggerFactory.getLogger(BotLauncher.class);

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("numbots: ").append(numbots).append("\nserver: ")
				.append(server).append("\nsecurityKey: ").append(securityKey)
				.append("\nmeeting: ").append(meeting)
				.append("\nvideoFilename: ").append(videoFilename)
				.append("\nvoiceFilename: ").append(voiceFilename)
				.append("\nget_meetings: ").append(get_meetings)
				.append("\ncommand_create: ").append(command_create)
				.append("\nname: ").append(name).append("\nrole: ")
				.append(role).append("\nprobabilities: ").append(probabilities)
				.append("\ninterval: ").append(interval)
				.append("\nsingle_meeting: ").append(single_meeting)
				.append("\neveryone_sends_video: ")
				.append(everyone_sends_video)
				.append("\nonly_one_sends_video: ")
				.append(only_one_sends_video)
				.append("\neveryone_receives_video: ")
				.append(everyone_receives_video)
				.append("\neveryone_sends_audio: ")
				.append(everyone_sends_audio)
				.append("\nonly_one_sends_audio: ")
				.append(only_one_sends_audio)
				.append("\neveryone_receives_audio: ")
				.append(everyone_receives_audio).append("\nbotArmy: ")
				.append(botArmy).append("\nprob_acc: ").append(prob_acc);
		return builder.toString();
	}

	@Parameter(names = "--numbots", description = "Number of bots")
	private int numbots = 0;
	@Parameter(names = "--server", description = "Server address", required = true)
	private String server;
	@Parameter(names = "--key", description = "Server security key", required = true)
	private String securityKey;
	@Parameter(names = "--meeting", description = "Meeting ID to spawn the bots")
	private String meeting = "Test meeting";
	@Parameter(names = "--video", description = "Video filename to be sent")
	private String videoFilename = null;
	@Parameter(names = "--audio", description = "Audio filename to be sent")
	private String voiceFilename = null;
	@Parameter(names = "--get_meetings", description = "Displays the open rooms")
	private boolean get_meetings = false;
	@Parameter(names = "--create", arity = 1, description = "If the meeting specified by --meeting doesn't exists, the bot will create it before join", validateWith = BooleanValidator.class)
	private boolean command_create = true;
	@Parameter(names = "--name", description = "Prefix of the bots followed by a number")
	private String name = "Bot";
	@Parameter(names = "--role", description = "Role of the bots in the conference (moderator|viewer)", validateWith = RoleValidator.class)
	private String role = "moderator";
	@Parameter(names = "--probabilities", description = "Specifies the probabilities for number of users per meeting", validateWith = ProbabilitiesValidator.class, converter = ProbabilitiesConverter.class)
	private Map<Integer, Double> probabilities;
	
	@Parameter(names = "--interval", description = "Interval between the launch of each bot (in milliseconds)")
	private int interval = 5000;
	@Parameter(names = "--single_meeting", arity = 1, description = "If set, all the bots will join the same room specified by --meeting", validateWith = BooleanValidator.class)
	private boolean single_meeting = false;
	
	@Parameter(names = "--everyone_sends_video", arity = 1, description = "If [true], all the bots will send the video file specified by --video", validateWith = BooleanValidator.class)
	private boolean everyone_sends_video = false;
	@Parameter(names = "--only_one_sends_video", arity = 1, description = "If [true], only one bot will send the video file specified by --video", validateWith = BooleanValidator.class)
	private boolean only_one_sends_video = true;
	@Parameter(names = "--everyone_receives_video", arity = 1, description = "If [true], all the bots will receive the video stream from the others", validateWith = BooleanValidator.class)
	private boolean everyone_receives_video = true;
	@Parameter(names = "--everyone_sends_audio", arity = 1, description = "If [true], all the bots will send the audio file specified by --audio", validateWith = BooleanValidator.class)
	private boolean everyone_sends_audio = false;
	@Parameter(names = "--only_one_sends_audio", arity = 1, description = "If [true], only one bot will send the audio file specified by --audio", validateWith = BooleanValidator.class)
	private boolean only_one_sends_audio = true;
	@Parameter(names = "--everyone_receives_audio", arity = 1, description = "If [true], all the bots will receive the audio stream from the others", validateWith = BooleanValidator.class)
	private boolean everyone_receives_audio = true;
	
	private List<Bot> botArmy = new ArrayList<Bot>();
	private HashMap<Integer, Double> prob_acc;

	private boolean parse(String[] args) throws IOException {
		JCommander parser = new JCommander(this);
		try {
			parser.parse(args);
		} catch (Exception e) {
			System.err.println(e.getMessage());
			parser.usage();
			return false;
		}

		// set the default probabilities
		if (probabilities == null) {
			probabilities = new HashMap<Integer, Double>();
			probabilities.put(2, 10.0);
			probabilities.put(3, 30.0);
			probabilities.put(4, 40.0);
			probabilities.put(5, 15.0);
			probabilities.put(6, 3.0);
			probabilities.put(7, 1.0);
			probabilities.put(10, 1.0);
		}
		
		double acc = 0.0;
		prob_acc = new HashMap<Integer, Double>();
		for (Entry<Integer, Double> entry : probabilities.entrySet()) {
			acc += entry.getValue();
			prob_acc.put(entry.getKey(), acc);
		}
		log.debug("Accumulated probabilities: {}", prob_acc.toString());
		
		role = role.toLowerCase();

		if (get_meetings) {
			BigBlueButtonClient client = new BigBlueButtonClient();
			client.createJoinService(server, securityKey);
			JoinServiceBase joinService = client.getJoinService();
			if (joinService == null) {
				log.error("Can't connect to the server, please check the server address");
				return false;
			}
			
			if (joinService.load() != JoinServiceBase.E_OK)
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
		return true;
	}
	
	public void spawnBots() throws InterruptedException {
		if (numbots <= 0) {
			log.info("Number of bots <= 0, quitting");
			return;
		}
		
		final Thread thread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				FlvPreLoader loader = new FlvPreLoader(videoFilename);
				
				log.info("Running with the following configuration:\n{}", BotLauncher.this.toString());
				
				Random seed = new Random();
				DecimalFormat name_format = new DecimalFormat(new String(new char[Integer.toString(numbots).length()]).replace("\0", "0"));
				DecimalFormat meeting_format = new DecimalFormat(new String(new char[3]).replace("\0", "0"));
				
				int meeting_index = 0;
				int remaining_bots = 0;
				boolean first_in_the_room = true;
				for (int bot_index = 1; bot_index <= numbots; ++bot_index) {
					String instance_meeting;
					if (single_meeting) {
						instance_meeting = meeting;
					} else {
						if (remaining_bots == 0) {
							meeting_index += 1;
							while (remaining_bots == 0) {
								double p = seed.nextDouble() * 100;
								for (Entry<Integer, Double> entry : prob_acc.entrySet()) {
									if (p < entry.getValue()) {
										remaining_bots = entry.getKey();
										log.debug("p = {}", p);
										break;
									}
								}
							}
							log.info("The next room will have {} participants", remaining_bots);
							first_in_the_room = true;
						}
						instance_meeting = meeting + " " + meeting_format.format(meeting_index);
						remaining_bots -= 1;
					}
					
					Bot bot = new Bot();
					botArmy.add(bot);
					String instance_name = name + " " + name_format.format(bot_index);
					
					log.info("Connecting a new bot called {} to the room {}", instance_name, instance_meeting);
					bot.setServer(server);
					bot.setSecurityKey(securityKey);
					bot.setMeetingId(instance_meeting);
					bot.setName(instance_name);
					bot.setRole(role);
					bot.setVideoFilename(videoFilename);
					bot.setAudioFilename(voiceFilename);
					bot.setSendVideo(everyone_sends_video || (only_one_sends_video && first_in_the_room));
					bot.setReceiveVideo(everyone_receives_video);
					bot.setSendAudio(everyone_sends_audio || (only_one_sends_audio && first_in_the_room));
					bot.setReceiveAudio(everyone_receives_audio);
					bot.setCreateMeeting(command_create && first_in_the_room);
					bot.setVideoLoader(loader);
					
					bot.start();

					if (interval > 0)
						try {
							Thread.sleep(interval);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					first_in_the_room = false;
				}
			}
		});
		thread.start();
		
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		try {
			String s = br.readLine();
			System.out.println(s);
		} catch (IOException exception) {
		}
		
		thread.join();

		for (Bot bot : botArmy) {
			bot.disconnect();
		}
		botArmy.clear();
	}

	public static void main (String[] args) throws IOException, InterruptedException {
		BotLauncher master = new BotLauncher();
		if (master.parse(args)) {
			master.spawnBots();
		}
	}
		
}
	