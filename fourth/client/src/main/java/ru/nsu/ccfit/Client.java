package ru.nsu.ccfit;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import okhttp3.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class Client
{
    private String host;
    private OkHttpClient client;
    private String token;
    private ArrayList<User> users;
    private ArrayList<Message> messages;
    private Type userListType = new TypeToken<ArrayList<User>>(){}.getType();
    private Type messageListType = new TypeToken<ArrayList<Message>>(){}.getType();
    private Gson g = new Gson();

    public Client(String host) {
        this.host = host;
        this.messages = new ArrayList<>();
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequestsPerHost(15);
        this.client = new OkHttpClient.Builder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .writeTimeout(1, TimeUnit.MINUTES)
                .readTimeout(1, TimeUnit.MINUTES)
                .dispatcher(dispatcher)
                .build();
    }

    public static void main(String[] args ){
        if (args.length < 1) {
            System.out.println("Need URL to chat server");
            System.exit(0);
        }
        Client client = new Client(args[0]);
        client.work();
    }

    public void work() {
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.println("Enter your name");
            while(token == null) {
                String username = reader.readLine();
                token = login(username);
                if (token == null) {
                    System.out.println("This name is busy, please choose another");
                }
            }
            int lastMessage = 0;
            int iter = 0;
            String input = null;
            while (iter != -1) {
                userList();
                lastMessage = updateMessages(lastMessage);
                iter = 0;
                while ((!reader.ready())&&(iter < 10)) {
                    Thread.sleep(100);
                    iter++;
                }
                if (reader.ready()) {
                    input = reader.readLine();
                    if (input.equals("/list")) {
                        printUsers();
                    } else if (input.equals("/quit")) {
                        iter = -1;
                    } else {
                        sendMessage(input);
                    }
                }
            }
            logout();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            System.out.println("Disconnected (reason - timeout)");
        }
    }

    private String login(String username) {
        HttpUrl route = HttpUrl.parse(host+"/login");
        Request request = new Request.Builder()
                .url(route)
                .post(RequestBody.create(MediaType.parse("application/json"),"{\"username\":\""+username+"\"}"))
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 200) {
                JsonObject body = new JsonParser().parse(response.body().string()).getAsJsonObject();
                return body.get("token").getAsString();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void logout() {
        HttpUrl route = HttpUrl.parse(host+"/logout");
        Request request = new Request.Builder()
                .url(route)
                .post(RequestBody.create(MediaType.parse("application/json"),""))
                .addHeader("Authorization","Token "+token)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.code() != 200) {
                throw new IllegalArgumentException();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void userList() {
        HttpUrl route = HttpUrl.parse(host+"/users");
        Request request = new Request.Builder()
                .url(route)
                .get()
                .addHeader("Authorization","Token "+token)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 200) {
                String body = response.body().string();
                body = body.substring(body.indexOf('['),body.length()-1);
                ArrayList<User> newUsers = g.fromJson(body, userListType);
                updateList(newUsers);
            } else {
                throw new IllegalArgumentException();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateList(ArrayList<User> newUsers) {
        for(int i = 0; i < users.size(); i++) {
            if (newUsers.get(i).isAlive() == null) {
                if (users.get(i).isAlive() != null) {
                    System.out.println("User "+users.get(i).getUsername()+" timeouted");
                }
            }else if (users.get(i).isAlive() == null) {
                if (newUsers.get(i).isAlive() != null) {
                    System.out.println("User "+users.get(i).getUsername()+" connected");
                }
            }
        }
        for (int i = users.size(); i < newUsers.size(); i++) {
            System.out.println("User "+newUsers.get(i).getUsername()+" connected");
        }
        users = newUsers;
    }


    private int updateMessages(int startOffset) {
        getMessages(startOffset);
        for(int i = startOffset; i < messages.size(); i++) {
            Message message = messages.get(i);
            String author = findAuthorByID(message.getAuthorID());
            System.out.println(author+": "+message.getMessage());
        }
        return messages.size();
    }

    private void getMessages(int startOffset) {
        ArrayList<Message> buffer = new ArrayList<>();
        int i = 0;
        while((i == 0) ||(buffer.size() != 0)) {
            buffer = new ArrayList<>();
            HttpUrl route = HttpUrl.parse(host + "/messages?offset=" + String.valueOf(100 * i + startOffset) + "&count=100");
            Request request = new Request.Builder()
                    .url(route)
                    .get()
                    .addHeader("Authorization", "Token " + token)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.code() == 200) {
                    String body = response.body().string();
                    body = body.substring(body.indexOf('['), body.length() - 1);
                    buffer = g.fromJson(body, messageListType);
                    messages.addAll(buffer);
                } else {
                    throw new IllegalArgumentException();
                }
                i++;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String findAuthorByID(long id) {
        for (User user : users) {
            if (user.getId() == id) {
                return user.getUsername();
            }
        }
        return "<Unknown>";
    }

    private void sendMessage(String text) {
        HttpUrl route = HttpUrl.parse(host + "/messages");
        Request request = new Request.Builder()
                .url(route)
                .post(RequestBody.create(MediaType.parse("application/json"), "{\"message\":\"" + text + "\"}"))
                .addHeader("Authorization", "Token " + token)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.code() != 200) {
                throw new IllegalArgumentException();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void printUsers() {
        System.out.println("Current users");
        for (User user : users) {
            if (user.isAlive()) {
                System.out.println();
            }
        }
    }
}
