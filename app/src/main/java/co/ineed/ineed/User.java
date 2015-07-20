package co.ineed.ineed;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by John on 12/07/2015.
 */
public class User implements Serializable {
    public String email;
    public String password;
    public String firstName;
    public String surname;
    public String phone;
    public Boolean isPaymentEnabled;
    public String id;
    public Card card;
    public Double credit;
    public String creditExpiryDate;
    public Boolean isRegistered;
    public List<RequestHistory> requestHistory = new ArrayList<RequestHistory>();
}
