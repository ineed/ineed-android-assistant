package co.ineed.ineed;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by John on 15/07/2015.
 */
public class Request implements Serializable {
    public String id;
    public String createdTime;
    public String taskId;
    public List<FormElements> form = new ArrayList<FormElements>();
    public List<Message> messages = new ArrayList<Message>();
    public List<Message> notes = new ArrayList<Message>();
    public String name;
    public String status;
    public String displayStatus;
    public String userId;
    public List<Charge> charges = new ArrayList<Charge>();
    public String assistantId;
    public String assistantName;
}
