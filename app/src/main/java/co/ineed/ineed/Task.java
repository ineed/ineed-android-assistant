package co.ineed.ineed;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by John on 12/07/2015.
 */
public class Task implements Serializable {
    public String id;
    public List<FormElements> formElements = new ArrayList<FormElements>();
    public Display display;
    public String tagId;
    public String name;

}
