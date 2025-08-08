package org.sergey_white.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Ticket {
    @JsonProperty("origin")
    private String origin;
    @JsonProperty("origin_name")
    private String originName;
    @JsonProperty("destination")
    private String destination;
    @JsonProperty("destination_name")
    private String destinationName;
    @JsonProperty("departure_date")
    private String departureDate;
    @JsonProperty("departure_time")
    private String departureTime;
    @JsonProperty("arrival_date")
    private String arrivalDate;
    @JsonProperty("arrival_time")
    private String arrivalTime;
    @JsonProperty("carrier")
    private String carrier;
    @JsonProperty("stops")
    private int stops;
    @JsonProperty("price")
    private BigDecimal price;

    private LocalDateTime departureDateTime;
    private LocalDateTime arrivalDateTime;
}