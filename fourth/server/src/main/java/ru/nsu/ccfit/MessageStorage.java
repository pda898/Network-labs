package ru.nsu.ccfit;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MessageStorage {
    private ConcurrentHashMap<Integer,Message> messageMap;
    private int currID;

    public MessageStorage() {
        this.messageMap = new ConcurrentHashMap<>();
        this.currID = 0;
    }

    public Message create(String text, long authorID) {
        Message message = new Message(currID,text,authorID);
        messageMap.put(currID,message);
        currID++;
        return message;
    }

    public List<Message> getAll(int offset, int count) {
        int start = Integer.max(Integer.min(offset,currID-1),0);
        int end = Integer.max(Integer.min(offset+count,currID),0);
        return messageMap.values().stream().sorted(Comparator.comparing((Message m) -> m.getId())).collect(Collectors.toList()).subList(start,end);
    }
}
