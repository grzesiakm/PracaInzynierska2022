package ghs;

import java.util.ArrayList;
import java.util.List;

public class Message {
    private int receiverId;
    private int senderId;
    private String type;
    private List<Integer> parameters = new ArrayList<>();


    public int getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(int receiverId) {
        this.receiverId = receiverId;
    }

    public int getSenderId() {
        return senderId;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<Integer> getParameters() {
        return parameters;
    }

    public void setParameters(List<Integer> parameters) {
        this.parameters = parameters;
    }

    @Override
    public String toString() {
        String params = "";
        if (parameters == null){
            params = "null";
        } else{
            params = parameters.toString();
        }
        return "Message " + getType() + " and parameters:  " + params;
    }
}
