package com.food.ordering.system.order.service.domain;

import com.food.ordering.system.order.service.domain.entity.*;
import com.food.ordering.system.order.service.domain.event.*;
import com.food.ordering.system.order.service.domain.exception.OrderDomainException;
import lombok.extern.slf4j.Slf4j;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

@Slf4j
public class OrderDomainServiceImpl implements OrderDomainService {

    public static final String UTC = "UTC";

    @Override
    public OrderCreatedEvent validateAndInitiateOrder(Order order, Restaurant restaurant) {
        validateRestaurant(restaurant);
        setOrderProductInformation(order, restaurant);
        order.validateOrder();
        order.initializeOrder();
        log.info("Order with id {} is initiated", order.getId().getValue());
        return new OrderCreatedEvent(order, ZonedDateTime.now(ZoneId.of(UTC)));
    }

    @Override
    public OrderPaidEvent payOrder(Order order) {
        order.pay();
        log.info("Order with id {} is paid", order.getId().getValue());
        return new OrderPaidEvent(order, ZonedDateTime.now(ZoneId.of(UTC)));
    }

    @Override
    public void approveOrder(Order order) {
        order.approve();
        log.info("Order with id {} is approved", order.getId().getValue());
    }

    @Override
    public OrderCancelledEvent cancelOrderPayment(Order order, List<String> failureMessages) {
        order.initCancelling(failureMessages);
        log.info("Order payment is cancelling for order with id {}", order.getId().getValue());
        return new OrderCancelledEvent(order, ZonedDateTime.now(ZoneId.of(UTC)));
    }

    @Override
    public void cancelOrder(Order order, List<String> failureMessages) {
        order.cancel(failureMessages);
        log.info("Order with id {} is cancelled", order.getId().getValue());
    }

    private void validateRestaurant(Restaurant restaurant) {
        if (!restaurant.isActive()) {
            throw new OrderDomainException("Restaurant with id " + restaurant.getId().getValue() + " is not active");
        }
    }

    private void setOrderProductInformation(Order order, Restaurant restaurant) {
        Map<Product, Product> restaurantProducts = restaurant.getProducts().stream()
                .collect(toMap(Function.identity(), Function.identity()));
        order.getItems().forEach(orderItem -> {
            Product currentProduct = orderItem.getProduct();
            if (restaurantProducts.containsKey(currentProduct)) {
                Product restaurantProduct = restaurantProducts.get(currentProduct);
                currentProduct.updateWithConfirmedNameAndPrice(restaurantProduct.getName(), restaurantProduct.getPrice());
            }
        });
    }
}