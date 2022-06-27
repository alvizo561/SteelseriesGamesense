package com.example;

import com.google.gson.JsonObject;

public class GameEvent {
    private String game ;
    private TrackedStats gameEvent;
    private int value;

    public GameEvent(TrackedStats gameEvent, int value) {
        this.game = GamesensePlugin.game;
        this.gameEvent = gameEvent;
        this.value = value;
    }
    public JsonObject buildJson(){
        JsonObject object = new JsonObject();
        object.addProperty("game",game);
        object.addProperty("event",gameEvent.name());
        JsonObject data = new JsonObject();
        data.addProperty("value",value);
        object.add("data",data);
        return object;
    }

    public String toJsonString(){

        return buildJson().toString();
    }

}
