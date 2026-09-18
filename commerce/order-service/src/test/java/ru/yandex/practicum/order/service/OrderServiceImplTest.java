package ru.yandex.practicum.order.service;

import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import ru.yandex.practicum.commerce.api.inventory.ReserveResponse;
import ru.yandex.practicum.commerce.api.order.CreateOrderRequest;
import ru.yandex.practicum.commerce.api.order.OrderDto;
import ru.yandex.practicum.commerce.api.order.OrderItemRequest;
import ru.yandex.practicum.commerce.api.product.CategoryDto;
import ru.yandex.practicum.commerce.api.product.ProductDto;
import ru.yandex.practicum.order.exception.InventoryServiceUnavailableException;
import ru.yandex.practicum.order.exception.OrderProcessingException;
import ru.yandex.practicum.order.exception.ProductServiceUnavailableException;
import ru.yandex.practicum.order.feign.InventoryClient;
import ru.yandex.practicum.order.feign.ProductClient;
import ru.yandex.practicum.order.repository.OrderRepository;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductClient productClient;

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private TransactionStatus transactionStatus;

    private OrderServiceImpl orderService;

    private ProductDto activeProduct;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(orderRepository, productClient, inventoryClient, transactionTemplate);

        activeProduct = new ProductDto(
                1L,
                "Smart Lamp",
                "Умная светодиодная лампа",
                new BigDecimal("1490.00"),
                new CategoryDto(1L, "Освещение", "Свет"),
                "http://example.com/lamp.png",
                true
        );

        // Настраиваем TransactionTemplate так, чтобы он выполнял переданный callback
        lenient().when(transactionTemplate.execute(any()))
                .thenAnswer(invocation -> {
                    TransactionCallback<?> callback = invocation.getArgument(0);
                    return callback.doInTransaction(transactionStatus);
                });
    }

    @Test
    @DisplayName("Успешный сценарий: заказ CONFIRMED")
    void shouldSaveConfirmedOrderWhenAllSuccess() {
        CreateOrderRequest request = new CreateOrderRequest(
                "Иван", "ivan@example.com",
                List.of(new OrderItemRequest(1L, 2))
        );

        when(productClient.getProductById(1L)).thenReturn(activeProduct);
        when(inventoryClient.reserveStock(any()))
                .thenReturn(new ReserveResponse(true, 8, "OK"));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrderDto result = orderService.create(request);

        assertThat(result.status()).isEqualTo("CONFIRMED");
        assertThat(result.totalPrice()).isEqualByComparingTo("2980.00");
    }

    @Test
    @DisplayName("Техническая недоступность product-service: заказ PENDING_CONFIRMATION")
    void shouldSavePendingOrderWhenProductServiceUnavailable() {
        CreateOrderRequest request = new CreateOrderRequest(
                "Иван", "ivan@example.com",
                List.of(new OrderItemRequest(1L, 2))
        );

        // Симулируем техническую деградацию через fallback
        when(productClient.getProductById(1L))
                .thenThrow(new ProductServiceUnavailableException(
                        "Product service unavailable for productId=1",
                        new RuntimeException("Connection refused")));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrderDto result = orderService.create(request);

        assertThat(result.status()).isEqualTo("PENDING_CONFIRMATION");
        assertThat(result.totalPrice()).isEqualByComparingTo("0");
        assertThat(result.items().get(0).productName())
                .contains("Product #1")
                .contains("pending verification");

        // Резервирование не вызывалось
        verify(inventoryClient, never()).reserveStock(any());
        verify(orderRepository).save(any());
    }

    @Test
    @DisplayName("Техническая недоступность inventory-service: заказ PENDING_CONFIRMATION")
    void shouldSavePendingOrderWhenInventoryServiceUnavailable() {
        CreateOrderRequest request = new CreateOrderRequest(
                "Иван", "ivan@example.com",
                List.of(new OrderItemRequest(1L, 2))
        );

        when(productClient.getProductById(1L)).thenReturn(activeProduct);
        when(inventoryClient.reserveStock(any()))
                .thenThrow(new InventoryServiceUnavailableException(
                        "Inventory service unavailable for productId=1",
                        new RuntimeException("Connection refused")));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrderDto result = orderService.create(request);

        assertThat(result.status()).isEqualTo("PENDING_CONFIRMATION");
        assertThat(result.totalPrice()).isEqualByComparingTo("2980.00"); // Цена есть
        assertThat(result.items().get(0).productName()).isEqualTo("Smart Lamp");
    }

    @Test
    @DisplayName("Бизнес-ошибка (товар не найден): заказ НЕ создаётся")
    void shouldNotSaveOrderWhenProductNotFound() {
        CreateOrderRequest request = new CreateOrderRequest(
                "Иван", "ivan@example.com",
                List.of(new OrderItemRequest(999L, 1))
        );

        when(productClient.getProductById(999L))
                .thenThrow(mock(FeignException.NotFound.class));

        assertThatThrownBy(() -> orderService.create(request))
                .isInstanceOf(OrderProcessingException.class)
                .hasMessageContaining("Product not found");

        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Бизнес-ошибка (недостаточно товара): заказ НЕ создаётся")
    void shouldNotSaveOrderWhenInsufficientStock() {
        CreateOrderRequest request = new CreateOrderRequest(
                "Иван", "ivan@example.com",
                List.of(new OrderItemRequest(1L, 100))
        );

        when(productClient.getProductById(1L)).thenReturn(activeProduct);
        when(inventoryClient.reserveStock(any()))
                .thenThrow(mock(FeignException.Conflict.class));

        assertThatThrownBy(() -> orderService.create(request))
                .isInstanceOf(OrderProcessingException.class)
                .hasMessageContaining("Insufficient stock");

        verify(orderRepository, never()).save(any());
    }
}