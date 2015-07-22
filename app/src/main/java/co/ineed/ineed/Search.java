package co.ineed.ineed;

import java.io.Serializable;

/**
 * Created by John on 22/07/2015.
 */
public class Search implements Serializable {
    public String type;
    public Task task;
    public Tag tag;
    public Item item;
}
