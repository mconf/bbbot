//BigBlueButtonBot, GT-MCONF @PRAV-UFRGS, developed by Arthur C. Rauter, august 2011.
//Adding video to the bots through Xuggler library, September 2011.
package org.mconf.bbb.bot;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mconf.bbb.BigBlueButtonClient;
import org.mconf.bbb.api.JoinServiceBase;
import org.mconf.bbb.api.Meeting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

public class BotLauncher {
	private static final Logger log = LoggerFactory.getLogger(BotLauncher.class);

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("numbots: ");
		builder.append(numbots);
		builder.append("\nserver: ");
		builder.append(server);
		builder.append("\nsecurityKey: ");
		builder.append(securityKey);
		builder.append("\nmeeting: ");
		builder.append(meeting);
		builder.append("\nvideoFilename: ");
		builder.append(videoFilename);
		builder.append("\nvoiceFilename: ");
		builder.append(voiceFilename);
		builder.append("\nget_meetings: ");
		builder.append(get_meetings);
		builder.append("\ncommand_create: ");
		builder.append(command_create);
		builder.append("\nrecord: ");
		builder.append(record);
		builder.append("\nname: ");
		builder.append(name);
		builder.append("\nrole: ");
		builder.append(role);
		builder.append("\nprobabilities: ");
		builder.append(probabilities);
		builder.append("\ninterval: ");
		builder.append(interval);
		builder.append("\nsingle_meeting: ");
		builder.append(single_meeting);
		builder.append("\neveryone_sends_video: ");
		builder.append(everyone_sends_video);
		builder.append("\nonly_one_sends_video: ");
		builder.append(only_one_sends_video);
		builder.append("\neveryone_receives_video: ");
		builder.append(everyone_receives_video);
		builder.append("\neveryone_sends_audio: ");
		builder.append(everyone_sends_audio);
		builder.append("\nonly_one_sends_audio: ");
		builder.append(only_one_sends_audio);
		builder.append("\neveryone_receives_audio: ");
		builder.append(everyone_receives_audio);
		builder.append("\neveryone_receives_deskshare: ");
		builder.append(everyone_receives_deskshare);
		builder.append("\nrecord_audio: ");
		builder.append(record_audio);
		builder.append("\naudio_sample_size: ");
		builder.append(audio_sample_size);
		builder.append("\nnumber_of_audio_samples: ");
		builder.append(number_of_audio_samples);
		builder.append("\nfill_last_room: ");
		builder.append(fill_last_room);
		builder.append("\nprint_rooms_info: ");
		builder.append(print_rooms_info);
		builder.append("\nlisten_only: ");
		builder.append(listen_only);
		builder.append("\ntwo_way_audio: ");
		builder.append(two_way_audio);
		builder.append("\nfinish_spawn_bots_thread: ");
		builder.append(finish_spawn_bots_thread);
		builder.append("\nfinished_spawn_bots_thread: ");
		builder.append(finished_spawn_bots_thread);
		builder.append("\nbotArmy: ");
		builder.append(botArmy);
		builder.append("\nprob_acc: ");
		builder.append(prob_acc);
		builder.append("\nmeetings: ");
		builder.append(meetings);
		builder.append("\nprintNumberOfParticipants: ");
		builder.append(printNumberOfParticipants);
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
	@Parameter(names = "--record", arity = 1, description = "Specifies record flag in the CREATE call", validateWith = BooleanValidator.class)
	private boolean record = true;
	@Parameter(names = "--name", description = "Prefix of the bots followed by a number")
	private String name = "Bot";
	@Parameter(names = "--role", description = "Role of the bots in the conference (moderator|viewer)", validateWith = RoleValidator.class)
	private String role = "viewer";
	@Parameter(names = "--probabilities", description = "Specifies the probabilities for number of users per meeting", validateWith = ProbabilitiesValidator.class, converter = ProbabilitiesConverter.class)
	private Map<Integer, Double> probabilities;
	
	@Parameter(names = "--interval", description = "Interval between the launch of each bot (in milliseconds)")
	private int interval = 5000;
	@Parameter(names = "--single_meeting", description = "If set, all the bots will join the same room specified by --meeting")
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

	@Parameter(names = "--everyone_receives_deskshare", arity = 1, description = "If [true], all the bots will connect the deskshare stream", validateWith = BooleanValidator.class)
	private boolean everyone_receives_deskshare = true;

	@Parameter(names = "--record_audio", description = "If set, only one bot will be launched to record the audio of a meeting")
	private boolean record_audio = false;
	@Parameter(names = "--audio_sample_size", description = "Size of the audio sample to record (in milliseconds)")
	private int audio_sample_size = 10000;
	@Parameter(names = "--number_of_audio_samples", description = "Number of samples to record by the bot")
	private int number_of_audio_samples = 1;
	
	@Parameter(names = "--fill_last_room", description = "If set, the last room will always be filled independently of the number of bots")
	private boolean fill_last_room = false;
	@Parameter(names = "--print_rooms_info", description = "If set, the information about number of participants per room will be printed to stdout")
	private boolean print_rooms_info = false;

	@Parameter(names = "--listen_only", description = "If set, all the bots will connect to the listen only stream")
	private boolean listen_only = false;
	@Parameter(names = "--two_way_audio", description = "If set, all the bots will connect to the two way audio stream")
	private boolean two_way_audio = false;

	private boolean finish_spawn_bots_thread = false;
	private boolean finished_spawn_bots_thread = false;

	private List<Bot> botArmy = new ArrayList<Bot>();
	private HashMap<Integer, Double> prob_acc;
	private List<Meeting> meetings;
	private Thread printNumberOfParticipants;

	private boolean parse(String[] args) throws IOException {
		JCommander parser = new JCommander(this);
		try {
			parser.parse(args);
		} catch (Exception e) {
			log.error(e.getMessage());
			parser.usage();
			return false;
		}

		// set the default probabilities
		if (probabilities == null) {
			probabilities = new HashMap<Integer, Double>();
			probabilities.put(2, 58.68);
			probabilities.put(3, 20.82);
			probabilities.put(4, 10.14);
			probabilities.put(5, 4.56);
			probabilities.put(6, 2.74);
			probabilities.put(7, 1.21);
			probabilities.put(8, 0.8);
			probabilities.put(9, 1.05);
		}
		
		double acc = 0.0;
		prob_acc = new HashMap<Integer, Double>();
		for (Entry<Integer, Double> entry : probabilities.entrySet()) {
			acc += entry.getValue();
			prob_acc.put(entry.getKey(), acc);
		}
		log.debug("Accumulated probabilities: {}", prob_acc.toString());
		
		role = role.toLowerCase();
		
		if (only_one_sends_audio && everyone_sends_audio) {
			log.error("--only_one_sends_audio and --everyone_sends_audio must not be true at the same time");
			return false;
		}
		if (only_one_sends_video && everyone_sends_video) {
			log.error("--only_one_sends_video and --everyone_sends_video must not be true at the same time");
			return false;
		}
		
		if (record_audio) {
			numbots = 1;
			command_create = false;
			single_meeting = true;
			only_one_sends_video = false;
			only_one_sends_audio = false;
			everyone_sends_video = false;
			everyone_sends_audio = false;
			everyone_receives_video = false;
			everyone_receives_audio = true;
		}
		
		if (two_way_audio && listen_only) {
			log.error("--two_way_audio and --listen_only must not be true at the same time");
			return false;
		}
		listen_only = listen_only || !two_way_audio;
		two_way_audio = two_way_audio || !listen_only;

		BigBlueButtonClient client = new BigBlueButtonClient();
		client.createJoinService(server, securityKey);
		JoinServiceBase joinService = client.getJoinService();
		if (joinService == null) {
			log.error("Can't connect to the server, please check the server address");
			return false;
		}
		
		if (joinService.load() != JoinServiceBase.E_OK) {
			log.error("Can't load the join service");
			return false;
		}
		meetings = client.getJoinService().getMeetings();

		if (get_meetings) {
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
	
	public void spawnBots() throws InterruptedException, IOException {
		if (numbots <= 0) {
			log.info("Number of bots <= 0, quitting");
			return;
		}
		
		Thread printNumberOfParticipants = new Thread(new Runnable() {
			
			@Override
			public void run() {
				BigBlueButtonClient client = new BigBlueButtonClient();
				client.createJoinService(server, securityKey);
				JoinServiceBase joinService = client.getJoinService();
				if (joinService == null) {
					log.error("Can't connect to the server, please check the server address");
					return;
				}
				
				while (true) {
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						break;
					}
					if (joinService.load() != JoinServiceBase.E_OK) {
						log.error("Can't load the join service");
						continue;
					}
					int count = 0;
					for (Meeting m : joinService.getMeetings()) {
						int numParticipants = m.getAttendees().size();
						log.info("Participants on meeting \"{}\": {}", m.getMeetingID(), numParticipants);
						count += numParticipants;
					}
					log.info("Total number of participants: {}", count);
				}
			}
		});
		if (print_rooms_info) {
			printNumberOfParticipants.start();
		}

		final Thread spawn_bots_thread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				finished_spawn_bots_thread = false;
				FlvPreLoader loader = null;
				if (videoFilename != null)
					loader = new FlvPreLoader(videoFilename);
				
				log.info("Running with the following configuration:\n{}", BotLauncher.this.toString());
				
				Random seed = new Random();
				DecimalFormat name_format = new DecimalFormat(new String(new char[Integer.toString(numbots).length()]).replace("\0", "0"));
				DecimalFormat meeting_format = new DecimalFormat(new String(new char[3]).replace("\0", "0"));
				
				int meeting_index = 0;
				// it will search for the meeting index considering the opened meetings
				Pattern pattern = Pattern.compile(meeting + " [0]*(\\d+)");
				for (Meeting meeting : meetings) {
					if (meeting.getParticipantCount() <= 0)
						continue;
					Matcher m = pattern.matcher(meeting.getMeetingID());
					if (m.matches()) {
						int candidate = Integer.parseInt(m.group(1));
						if (candidate > meeting_index)
							meeting_index = candidate;
					}
				}
				
				int remaining_bots = 0;
				boolean first_in_the_room = true;
				int bot_index = 1;
				while ((bot_index <= numbots || (fill_last_room && remaining_bots != 0)) && !finish_spawn_bots_thread) {
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
					bot.setReceiveDeskshare(everyone_receives_deskshare);
					bot.setRecordAudio(record_audio);
					bot.setAudioSampleSize(audio_sample_size);
					bot.setNumberOfAudioSamples(number_of_audio_samples);
					bot.setCreateMeeting(command_create && first_in_the_room);
					bot.setVideoLoader(loader);
					bot.setListenOnly(listen_only && !(everyone_sends_audio || (only_one_sends_audio && first_in_the_room)));
					bot.setRecord(record);
					
					bot.start();

					if (interval > 0)
						try {
							Thread.sleep(interval);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					first_in_the_room = false;
					
					bot_index++;
				}
				finished_spawn_bots_thread = true;
			}
		});
		
//		Runtime.getRuntime().addShutdownHook(new Thread() {
//			@Override
//			public void run() {
//				log.info("Finalizing bots...");
//				finalize_spawn_bots_thread = true;
//				try {
//					spawn_bots_thread.join();
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//				for (Bot bot : botArmy) {
//					bot.disconnect();
//				}
//				log.info("Disconnected");
//				botArmy.clear();
//			}
//		});
		spawn_bots_thread.start();
		System.in.read();
		if (!finished_spawn_bots_thread) {
			log.info("Stopping to spawn bots...");
			finish_spawn_bots_thread = true;
			spawn_bots_thread.join();
			log.info("New bots won't join anymore...");
		} else {
			log.info("All bots already joined meetings...");
		}
		log.info("Press ENTER again to disconnect everybody...");
		System.in.read();
		for (Bot bot : botArmy) {
			bot.disconnect();
		}
		log.info("Disconnected!");
		botArmy.clear();

		printNumberOfParticipants.interrupt();
		try {
			printNumberOfParticipants.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void main (String[] args) throws IOException, InterruptedException {
		BotLauncher master = new BotLauncher();
		if (master.parse(args)) {
			master.spawnBots();
		}
	}
		
}
	
