package org.sergey_white.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.sergey_white.entity.Ticket;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlyAnalyzer {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public void analyze(String fileName) {
        try {

            List<Ticket> tickets = readTicketsFromFile(fileName);
            if (tickets.isEmpty()) {
                System.out.println("Нет данных о рейсах в файле.");
                return;
            }

            Map<String, Long> minFlightTimes = calculateMinFlightTimes(tickets);
            printMinFlightTimes(minFlightTimes);

            List<Double> prices = extractPrices(tickets);
            printPriceStatistics(prices);

        } catch (IOException e) {
            System.err.println("Ошибка при обработке файла: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Читает и парсит JSON-файл tickets.json из корневой директории, извлекает только нужные для анализа данные.
     * @return Список объектов Ticket.
     * @throws IOException Если файл не найден или не удалось разобрать JSON.
     */
    private List<Ticket> readTicketsFromFile(String fileName) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        File jsonFile = new File(fileName);
        if (!jsonFile.exists()) {
            throw new IOException("Файл " + fileName + " не найден в текущей директории.");
        }

        JsonNode root = mapper.readTree(jsonFile);
        JsonNode ticketsNode = root.path("tickets");
        List<Ticket> tickets = new ArrayList<>();

        for (JsonNode node : ticketsNode) {
            Ticket ticket = new Ticket();
            ticket.setCarrier(node.path("carrier").asText());
            ticket.setDepartureDateTime(node.path("departureDateTime").asText());
            ticket.setArrivalDateTime(node.path("arrivalDateTime").asText());
            ticket.setPrice(node.path("price").asDouble(0.0));
            tickets.add(ticket);
        }

        return tickets;
    }

    /**
     * Рассчитывает минимальное время полета для каждого перевозчика.
     * @param tickets Список билетов.
     * @return Карта: перевозчик -> минимальное время полета в минутах.
     */
    private Map<String, Long> calculateMinFlightTimes(List<Ticket> tickets) {
        Map<String, Long> minFlightTimes = new HashMap<>();

        for (Ticket ticket : tickets) {
            try {
                LocalDateTime departure = LocalDateTime.parse(ticket.getDepartureDateTime(), FORMATTER);
                LocalDateTime arrival = LocalDateTime.parse(ticket.getArrivalDateTime(), FORMATTER);
                long duration = Duration.between(departure, arrival).toMinutes();
                minFlightTimes.compute(ticket.getCarrier(), (carrier, currentMin) ->
                        currentMin == null ? duration : Math.min(currentMin, duration));
            } catch (Exception e) {
                System.err.println("Ошибка парсинга времени для рейса: " + ticket.getCarrier());
            }
        }

        return minFlightTimes;
    }

    /**
     * Выводит минимальное время полета для каждого перевозчика.
     * @param minFlightTimes Карта с минимальным временем полета.
     */
    private void printMinFlightTimes(Map<String, Long> minFlightTimes) {
        System.out.println("Минимальное время полета для каждого перевозчика (в минутах):");
        if (minFlightTimes.isEmpty()) {
            System.out.println("Нет данных о времени полета.");
        } else {
            minFlightTimes.forEach((carrier, duration) ->
                    System.out.printf("%s: %d минут%n", carrier, duration));
        }
    }

    /**
     * Извлекает цены из списка билетов.
     * @param tickets Список билетов.
     * @return Отсортированный список ненулевых цен.
     */
    private List<Double> extractPrices(List<Ticket> tickets) {
        return tickets.stream()
                .map(Ticket::getPrice)
                .sorted()
                .toList();
    }

    /**
     * Вычисляет и выводит статистику цен: среднюю цену, медиану и их разницу.
     * @param prices Список цен.
     */
    private void printPriceStatistics(List<Double> prices) {
        if (prices.isEmpty()) {
            System.out.println("\nЦены не доступны в данных.");
            return;
        }

        double averagePrice = prices.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double medianPrice;
        int size = prices.size();
        if (size % 2 == 0) {
            medianPrice = (prices.get(size / 2 - 1) + prices.get(size / 2)) / 2.0;
        } else {
            medianPrice = prices.get(size / 2);
        }
        double priceDifference = Math.abs(averagePrice - medianPrice);

        System.out.printf("\nСредняя цена: %.2f%n", averagePrice);
        System.out.printf("Медиана цены: %.2f%n", medianPrice);
        System.out.printf("Разница между средней ценой и медианой: %.2f%n", priceDifference);
    }
}