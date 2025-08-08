package org.sergey_white.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.sergey_white.entity.Ticket;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.Duration;

public class FlyAnalyzer {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("[H:mm][HH:mm]");

    public void analyze(String fileName, String departurePoint, String arrivalPoint) {
        try {
            List<Ticket> tickets = readTicketsFromFile(fileName, departurePoint, arrivalPoint);
            if (tickets.isEmpty()) {
                System.out.println("Нет данных о рейсах в файле.");
                return;
            }

            Map<String, Long> minFlightTimes = calculateMinFlightTimes(tickets);
            printMinFlightTimes(minFlightTimes);

            List<BigDecimal> prices = extractPrices(tickets);
            printPriceStatistics(prices);

        } catch (IOException e) {
            System.err.println("Ошибка при обработке файла: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<Ticket> readTicketsFromFile(String fileName, String departurePoint, String arrivePoint) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        File jsonFile = new File(fileName);
        if (!jsonFile.exists()) {
            throw new IOException("Файл " + fileName + " не найден в текущей директории.");
        }

        JsonNode root = mapper.readTree(jsonFile);
        JsonNode ticketsNode = root.path("tickets");
        List<Ticket> tickets = new ArrayList<>();

        for (JsonNode node : ticketsNode) {
            if (!isSearchFly(departurePoint, arrivePoint, node)) {
                continue;
            }
            Ticket ticket = new Ticket();
            ticket.setCarrier(node.path("carrier").asText());

            try {
                LocalDateTime departureDateTime = parseDateTime(
                        node.path("departure_date").asText(),
                        node.path("departure_time").asText(),
                        "отправления"
                );
                LocalDateTime arrivalDateTime = parseDateTime(
                        node.path("arrival_date").asText(),
                        node.path("arrival_time").asText(),
                        "прибытия"
                );

                ticket.setDepartureDateTime(departureDateTime);
                ticket.setArrivalDateTime(arrivalDateTime);
                BigDecimal price = new BigDecimal(node.path("price").asText("0"));
                if (price.compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException("Цена не может быть отрицательной: " + price);
                }
                ticket.setPrice(price);

                tickets.add(ticket);
            } catch (DateTimeParseException e) {
                System.err.println("Ошибка парсинга времени для рейса " + node.path("carrier").asText() + ": " + e.getMessage());
            } catch (NumberFormatException e) {
                System.err.println("Ошибка парсинга цены для рейса " + node.path("carrier").asText() + ": " + e.getMessage());
            }
        }
        return tickets;
    }

    private LocalDateTime parseDateTime(String dateStr, String timeStr, String timeType) {
        try {
            LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
            LocalTime time = LocalTime.parse(timeStr, TIME_FORMATTER);
            return LocalDateTime.of(date, time);
        } catch (DateTimeParseException e) {
            throw new DateTimeParseException(
                    "Ошибка парсинга " + timeType + " (дата: " + dateStr + ", время: " + timeStr + ")",
                    e.getParsedString(), e.getErrorIndex(), e
            );
        }
    }

    private boolean isSearchFly(String departurePoint, String arrivePoint, JsonNode node) {
        return node.path("origin_name").asText().equals(departurePoint)
                && node.path("destination_name").asText().equals(arrivePoint);
    }

    private Map<String, Long> calculateMinFlightTimes(List<Ticket> tickets) {
        Map<String, Long> minFlightTimes = new HashMap<>();

        for (Ticket ticket : tickets) {
            try {
                LocalDateTime departure = ticket.getDepartureDateTime();
                LocalDateTime arrival = ticket.getArrivalDateTime();
                Duration duration = Duration.between(departure, arrival);
                if (duration.isNegative()) {
                    duration = duration.plusDays(1);
                }
                long durationInMinutes = duration.toMinutes();
                minFlightTimes.compute(ticket.getCarrier(), (carrier, currentMin) ->
                        currentMin == null ? durationInMinutes : Math.min(currentMin, durationInMinutes));
            } catch (Exception e) {
                System.err.println("Ошибка расчета времени для рейса: " + ticket.getCarrier());
                e.printStackTrace();
            }
        }

        return minFlightTimes;
    }

    private void printMinFlightTimes(Map<String, Long> minFlightTimes) {
        System.out.println("Минимальное время полета для каждого перевозчика:");
        if (minFlightTimes.isEmpty()) {
            System.out.println("Нет данных о времени полета.");
        } else {
            minFlightTimes.forEach((carrier, duration) ->
                    System.out.printf("%s: %d часов %d минут%n",
                            carrier, duration / 60, duration % 60));
        }
    }

    private List<BigDecimal> extractPrices(List<Ticket> tickets) {
        return tickets.stream()
                .map(Ticket::getPrice)
                .sorted()
                .toList();
    }

    private void printPriceStatistics(List<BigDecimal> prices) {
        if (prices.isEmpty()) {
            System.out.println("\nЦены не доступны в данных.");
            return;
        }

        BigDecimal sum = prices.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal averagePrice = sum.divide(BigDecimal.valueOf(prices.size()), 0, RoundingMode.HALF_UP);

        BigDecimal medianPrice;
        int size = prices.size();
        if (size % 2 == 0) {
            BigDecimal lower = prices.get(size / 2 - 1);
            BigDecimal upper = prices.get(size / 2);
            medianPrice = lower.add(upper).divide(BigDecimal.valueOf(2), 0, RoundingMode.HALF_UP);
        } else {
            medianPrice = prices.get(size / 2);
        }

        BigDecimal priceDifference = averagePrice.subtract(medianPrice).abs();

        System.out.printf("\nСредняя цена: %s руб.%n", formatBigDecimal(averagePrice));
        System.out.printf("Медиана цены: %s руб.%n", formatBigDecimal(medianPrice));
        System.out.printf("Разница между средней ценой и медианой: %s руб.%n", formatBigDecimal(priceDifference));
    }

    private String formatBigDecimal(BigDecimal value) {
        if (value.scale() <= 0 || value.stripTrailingZeros().scale() <= 0) {
            return value.setScale(0, RoundingMode.UNNECESSARY).toString();
        }
        return value.setScale(2, RoundingMode.HALF_UP).toString();
    }
}