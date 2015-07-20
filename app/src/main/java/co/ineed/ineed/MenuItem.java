package co.ineed.ineed;

import android.content.Context;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by John on 12/07/2015.
 */
public class MenuItem implements Serializable {
    public String id;
    public String name;
    public String icon;
    public Content content;
    public Boolean isExpandable;
    public List<MenuItem> children = new ArrayList<MenuItem>();
}
