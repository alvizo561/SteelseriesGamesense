package com.example;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Provides;
import javax.inject.Inject;


import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;

import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;


import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

@Slf4j
@PluginDescriptor(
	name = "Steelseries Gamesense"
)
public class GamesensePlugin extends Plugin
{
	private String sse3Address;
	private final String game = "RUNELITE"; //required to identify the game on steelseries client

	//help vars to determine what should change
	private int lastXp =0;
	private int currentHp =0;
	private int currentPrayer =0;
	@Inject
	private Client client;

	@Inject
	private GamesenseConfig config;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Example started!");
		FindSSE3Port();	//finding the steelseries client port
		initGamesense(); //initialise the events that are displayable on the keyboard
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Example stopped!");
	}

	@Subscribe
	public void onStatChanged(StatChanged statChanged){

		if (statChanged.getSkill() == Skill.HITPOINTS){
			int lvl = statChanged.getBoostedLevel();
			int max = statChanged.getLevel();
			int percent = lvl *100 / max;
			currentHp = lvl;

			String msg ="{" +
					"  \"game\": \""+game+"\"," +
					"  \"event\": \"HEALTH\"," +
					"  \"data\": {\"value\": "+percent+"}" +
					"}" ;
			JsonObject jo = new Gson().fromJson(msg,JsonObject.class);

			executePost("game_event ",jo.toString());

		} if  (statChanged.getSkill() == Skill.PRAYER){

			int lvl = statChanged.getBoostedLevel();
			int max = statChanged.getLevel();
			int percent = lvl *100 / max;
			currentPrayer = lvl;

			String msg ="{" +
					"  \"game\": \""+game+"\"," +
					"  \"event\": \"PRAYER\"," +
					"  \"data\": {\"value\": "+percent+"}" +
					"}" ;
			JsonObject jo = new Gson().fromJson(msg,JsonObject.class);

			executePost("game_event ",jo.toString());
		}
		//if there was a change in XP we have had an xp drop
		if (statChanged.getXp() != lastXp) {
			if (statChanged.getSkill() == Skill.PRAYER && currentPrayer ==statChanged.getBoostedLevel()){}
			else if (statChanged.getSkill() == Skill.HITPOINTS && currentHp ==statChanged.getBoostedLevel()){}
			else {
				lastXp = statChanged.getXp();
				int start = getStartXpOfLvl(statChanged.getLevel());
				int end = getEndXPOfLvl(statChanged.getLevel());
				int percent = (lastXp-start) *100 / (end-start);

				if (percent > 100) {percent = 100;}
				String msg ="{" +
						"  \"game\": \""+game+"\"," +
						"  \"event\": \"CURRENTSKILL\"," +
						"  \"data\": {\"value\": "+percent+"}" +
						"}" ;
				JsonObject jo = new Gson().fromJson(msg,JsonObject.class);

				executePost("game_event ",jo.toString());
			}
		}




	}
	private void sendEnergy(){
		String msg ="{" +
				"  \"game\": \""+game+"\"," +
				"  \"event\": \"RUN_ENERGY\"," +
				"  \"data\": {\"value\": "+client.getEnergy()+"}" +
				"}" ;
		JsonObject jo = new Gson().fromJson(msg,JsonObject.class);
		executePost("game_event ",jo.toString());	//update the run energy
	}
	private void sendSpecialAttackPercent(){
		String msg ="{" +
				"  \"game\": \""+game+"\"," +
				"  \"event\": \"SPECIAL_ATTACK\"," +
				"  \"data\": {\"value\": "+client.getVar(VarPlayer.SPECIAL_ATTACK_PERCENT)/10+"}" +
				"}" ;
		JsonObject jo = new Gson().fromJson(msg,JsonObject.class);
		executePost("game_event ",jo.toString());	//update the run energy
	}

	@Subscribe
	public void onGameTick(GameTick tick){
		sendEnergy();
		sendSpecialAttackPercent();
	}


	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState().equals(GameState.HOPPING)|| gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{

			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Connected to: " + sse3Address, "Gamesense");
		}
	}
	@Provides
	GamesenseConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(GamesenseConfig.class);
	}


	//find the port to which we should connect
	private void FindSSE3Port() {

		// Open coreProps.json to parse what port SteelSeries Engine 3 is listening on.
		String jsonAddressStr = "";
		String corePropsFileName;
		// Check if we should be using the Windows path to coreProps.json
		if(System.getProperty("os.name").startsWith("Windows")) {
			corePropsFileName = System.getenv("PROGRAMDATA") +
					"\\SteelSeries\\SteelSeries Engine 3\\coreProps.json";
		} else {
			// Mac path to coreProps.json
			corePropsFileName = "/Library/Application Support/" +
					"SteelSeries Engine 3/coreProps.json";
		}

		try {
			BufferedReader coreProps = new BufferedReader(new FileReader(corePropsFileName));
			jsonAddressStr = coreProps.readLine();
			System.out.println("Opened coreProps.json and read: " + jsonAddressStr);
			coreProps.close();
		} catch (FileNotFoundException e) {
			System.out.println("coreProps.json not found");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Unhandled exception.");
		}
			// Save the address to SteelSeries Engine 3 for game events.
			if(!jsonAddressStr.equals("")) {
				//JSONObject obj = new JSONObject(jsonAddressStr);
				JsonObject obj = new Gson().fromJson(jsonAddressStr,JsonObject.class);
				sse3Address = "http://" + obj.get("address").getAsString();
			}

	}

	private void gameRegister(){
		String msg ="{" +
				"  \"game\": \""+game+"\"," +
				"  \"game_display_name\": \"Old School Runescape\"," +
				"  \"developer\": \"Gmoley\"" +
				"}";
		//JSONObject jo = new JSONObject(msg);
		JsonObject object = new Gson().fromJson(msg,JsonObject.class);

		executePost("game_metadata",object.toString());
	}
	private void registerStat(String event, int IconId){
		String msg = 	"{" +
				"  \"game\": \""+game+"\"," +
				"  \"event\": \""+event+"\"," +
				"  \"min_value\": 0," +
				"  \"max_value\": 100," +
				"  \"icon_id\": "+IconId+"," +
				"\"handlers\": [" +
				"    {" +
				"      \"device-type\": \"keyboard\"," +
				"      \"color\": {" +
				"        \"gradient\": {" +
				"          \"zero\": {" +
				"            \"red\": 0," +
				"            \"green\": 0, " +
				"            \"blue\": 0" +
				"          }," +
				"          \"hundred\": {" +
				"            \"red\": 0, " +
				"            \"green\": 255, " +
				"            \"blue\": 0" +
				"          }" +
				"        }" +
				"      }," +
				"      \"mode\": \"percent\"" +
				"    }" +
				"  ]"+
				"}";
		JsonObject object = new Gson().fromJson(msg,JsonObject.class);
		//JSONObject jo = new JSONObject(msg);

		executePost("register_game_event",object.toString());

	}

	private void initGamesense(){

			gameRegister();
			registerStat("HEALTH",38);
			registerStat("PRAYER",40);
			registerStat("CURRENTSKILL",13);
			registerStat("RUN_ENERGY",16);
			registerStat("SPECIAL_ATTACK",0);
	}


	public void executePost(String extraAddress, String jsonData) {

		//System.out.println(sse3Address);
		try {
			URL url = new URL(sse3Address +"/"+ extraAddress);
			// Create an HTTP connection to core
			HttpURLConnection connection = (HttpURLConnection)url.openConnection();
			// 1ms read timeout, as we don't care to read the result, just send & forget
			//connection.setReadTimeout(1);
			connection.setUseCaches(false);
			connection.setDoOutput(true);
			connection.setDoInput(true);
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setRequestProperty("Accept", "application/json");

			// Send the json data
			DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
			byte[] data = jsonData.getBytes(Charset.forName("UTF-8"));
			wr.write(data);
			try(BufferedReader br = new BufferedReader(
					new InputStreamReader(connection.getInputStream(), "utf-8"))) {
				StringBuilder response = new StringBuilder();
				String responseLine = null;
				while ((responseLine = br.readLine()) != null) {
					response.append(responseLine.trim());
				}
				//System.out.println(response.toString());
			}

			wr.flush();
			wr.close();
			// The following triggers the request to actually send. Just one of the quirks of HttpURLConnection.
			//connection.getInputStream();  // this line is crashing the requests
			// Done, make sure we disconnect
			connection.disconnect();

		} catch (Exception e) {
			 e.printStackTrace();
		}

	}
	private int getEndXPOfLvl(int lvl){
		int xp = getStartXpOfLvl(lvl) + getXpForLvl(lvl);

		return xp;
	}

	private int getStartXpOfLvl(int lvl){
		int total =0;
		for (int i =1;i<=lvl;i++){
			total += getXpForLvl(i);
		}

		return total;
	}

	private int getXpForLvl(int lvl){
		double exp = (float)(lvl-1)/7;
		double amount = 1.0/4.0 *((lvl-1) + 300*Math.pow(2.0,exp));

		return (int) amount;
	}
}
