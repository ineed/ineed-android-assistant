package co.ineed.ineed;

import java.io.Serializable;

/**
 * Created by John on 15/07/2015.
 */
public class Message implements Serializable {
    public String createdTime;
    public String text;
    public String name;
    public String profileImage;
    public Boolean isFromUser;
    public String image;
    public String link;
    public String type;
}
