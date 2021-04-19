package com.github.arci0066.worth.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.List;

public class ChatMessages {

    private String projectTitle;
    private List<String> messages;
    private Gson gson;

// ------ Constructors ------

    public ChatMessages(String projectTitle, String messages) {
        this.projectTitle = projectTitle;
        gson = new Gson();
        this.messages = gson.fromJson(messages, new TypeToken<List<String>>() {
        }.getType());
    }

// ------ Getters -------

    public String getProjectTitle() {
        return projectTitle;
    }

    public String getMessages() {
        String response= "";
        for (String str: messages) {
            response += str +"\n";
        }
        return response;
    }

    // ------ Methods ------
    public void add(String message) {
        messages.add(message);
    }
}
