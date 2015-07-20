package co.ineed.ineed;

import android.view.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by John on 12/07/2015.
 */
public class MenuItems implements Serializable {
    public List<MenuItem> items = new ArrayList<MenuItem>();
    public MenuItem general;
}
