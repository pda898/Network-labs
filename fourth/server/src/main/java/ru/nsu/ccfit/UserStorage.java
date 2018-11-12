package ru.nsu.ccfit;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class UserStorage {
    private CopyOnWriteArrayList<User> userList;
    private ConcurrentHashMap<String, Long> tokenMap;
    private Long currID;

    public UserStorage() {
        this.userList = new CopyOnWriteArrayList<>();
        this.tokenMap = new ConcurrentHashMap<>();
        this.currID = 0L;
    }

    public LoginInfo create(String username){
        for (User user : userList) {
            if (user.getUsername().equals(username)) {
                return null;
            }
        }
        User newUser = new User(currID,username);
        String token = generateToken(username);
        userList.add(newUser);
        tokenMap.put(token,currID);
        currID++;
        return new LoginInfo(newUser,token);
    }

    public User checkToken(String token) {
        Long id = tokenMap.get(token);
        if (id == null) {
            return null;
        }
        for (User user : userList) {
            if (user.getId() == id) {
                return user;
            }
        }
        return null;
    }

    public void delete(User user, String token) {
        userList.remove(user);
        tokenMap.remove(token);
    }

    public User get(long id){
        for (User user : userList) {
            user.check();
            if (user.getId() == id) {
                return user;
            }
        }
        return null;
    }

    public List<User> listUsers() {
        for (User user : userList) {
            user.check();
        }
        return userList;
    }

    private String generateToken(String string) {
        string = string+String.valueOf(System.currentTimeMillis());
        return String.valueOf(string.hashCode());
    }
}
