package org.sergey_white.entity;

import lombok.Data;

@Data
public class Ticket {

    private Passenger passenger;
    private String carrier;
    private String departureDateTime;
    private String arrivalDateTime;
    private double price;

}
