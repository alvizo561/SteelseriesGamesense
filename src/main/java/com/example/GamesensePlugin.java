package com.example;

import com.google.inject.Provides;
import javax.inject.Inject;

import jdk.internal.access.JavaNetHttpCookieAccess;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import org.json.JSONException;
import org.json.JSONObject;

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
	private String game = "RuneLite";
	@Inject
	private Client client;

	@Inject
	private GamesenseConfig config;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Example started!");
		FindSSE3Port();
		initGamesense();
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Example stopped!");
	}
	@Subscribe
	public void onStatChanged(StatChanged statChanged){

		if (statChanged.getSkill() == Skill.HITPOINTS){
			System.out.println("Current HP:" + statChanged.getBoostedLevel());
			String msg ="{" +
					"  \"game\": \""+game+"\"," +
					"  \"event\": \"HEALTH\"," +
					"  \"data\": {\"value\": "+statChanged.getBoostedLevel()+"}" +
					"}" ;
			JSONObject jo = new JSONObject(msg);
			System.out.println(jo.toString());
			executePost("game_event",jo.toString());

		} if  (statChanged.getSkill() == Skill.PRAYER){
			System.out.println("Current Prayer:" + statChanged.getBoostedLevel());
			String msg ="{" +
					"  \"game\": \""+game+"\"," +
					"  \"event\": \"PRAYER\"," +
					"  \"data\": {\"value\": "+statChanged.getBoostedLevel()+"}" +
					"}" ;
			JSONObject jo = new JSONObject(msg);
			System.out.println(jo.toString());
			executePost("game_event",jo.toString());
		}




	}
	@Subscribe
	public void onGameTick(GameTick tick){
		//Keepalive of gamesense --> gamesense requires an event at least every 15s, this counts as an event.
		//This way we keep the device lights active even if the user is AFK.
		executePost("game_heartbeat","{  \"game\": \""+game+"\"}");
	}


	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState().equals(GameState.HOPPING)|| gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			System.out.println(sse3Address);
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

		try {
			// Save the address to SteelSeries Engine 3 for game events.
			if(!jsonAddressStr.equals("")) {
				JSONObject obj = new JSONObject(jsonAddressStr);

				sse3Address = "http://" + obj.getString("address");
			}
		} catch (JSONException e) {
			e.printStackTrace();
			System.out.println("Exception creating JSONObject from coreProps.json.");
		}
	}

	private void gameRegister(){
		String msg ="{" +
				"  \"game\": \""+game+"\"," +
				"  \"game_display_name\": \"RuneLite\"," +
				"  \"developer\": \"Gmoley\"" +
				"}";
		JSONObject jo = new JSONObject(msg);
		System.out.println(jo.toString());
		executePost("game_metadata",jo.toString());
	}
	private void registerStat(String event, int IconId){
		String msg = 	"{" +
				"  \"game\": \""+game+"\"," +
				"  \"event\": \""+event+"\"," +
				"  \"min_value\": 0," +
				"  \"max_value\": 100," +
				"  \"icon_id\": "+IconId+"," +
				"\"value_optional\": false"+
				"}";
		JSONObject jo = new JSONObject(msg);
		System.out.println(jo.toString());
		executePost("game_metadata",jo.toString());
	}

	private void initGamesense(){
			gameRegister();
			registerStat("HEALTH",38);
			registerStat("PRAYER",40);
			registerStat("CURRENTSKILL",0);
	}


	public void executePost(String extraAddress, String jsonData) {
		try {
			URL url = new URL(sse3Address + extraAddress);
			// Create an HTTP connection to core
			HttpURLConnection connection = (HttpURLConnection)url.openConnection();
			// 1ms read timeout, as we don't care to read the result, just send & forget
			connection.setReadTimeout(1);
			connection.setUseCaches(false);
			connection.setDoOutput(true);
			connection.setDoInput(true);
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/json");

			// Send the json data
			DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
			byte[] data = jsonData.getBytes(Charset.forName("UTF-8"));
			wr.write(data);
			wr.flush();
			wr.close();
			// The following triggers the request to actually send. Just one of the quirks of HttpURLConnection.
			connection.getInputStream();
			// Done, make sure we disconnect
			connection.disconnect();

		} catch (Exception e) {
			// e.printStackTrace();
		}
	}
}
