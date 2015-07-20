package co.ineed.ineed;

import java.io.Serializable;

/**
 * Created by John on 12/07/2015.
 */
public class Content implements Serializable {
    public String type;
    public Task task;
    public Tag tag;
    public Item item;
}
