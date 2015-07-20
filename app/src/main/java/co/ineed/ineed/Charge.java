package co.ineed.ineed;

import java.io.Serializable;

/**
 * Created by John on 15/07/2015.
 */
public class Charge implements Serializable {
    public Integer amount;
    public Integer amountChargedToCard;
    public Card card;
    public PaymentResult paymentResult;
    public String createdTime;
    public String description;
}
