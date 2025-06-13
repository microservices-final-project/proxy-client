package com.selimhorri.app.business.payment.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import com.selimhorri.app.business.auth.enums.ResourceType;
import com.selimhorri.app.business.auth.util.AuthUtil;
import com.selimhorri.app.business.payment.model.OrderDto;
import com.selimhorri.app.business.payment.model.PaymentDto;
import com.selimhorri.app.business.payment.model.PaymentStatus;
import com.selimhorri.app.business.payment.model.response.PaymentPaymentServiceDtoCollectionResponse;
import com.selimhorri.app.business.payment.service.PaymentClientService;
import com.selimhorri.app.exception.wrapper.UnauthorizedException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentController Unit Tests")
class PaymentControllerTest {

    @Mock
    private AuthUtil authUtil;

    @Mock
    private PaymentClientService paymentClientService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private UserDetails userDetails;

    private PaymentController paymentController;
    private PaymentDto paymentDto;
    private OrderDto orderDto;
    private PaymentPaymentServiceDtoCollectionResponse collectionResponse;

    @BeforeEach
    void setUp() {
        // Create controller and inject mocks
        paymentController = new PaymentController(paymentClientService);
        ReflectionTestUtils.setField(paymentController, "authUtil", authUtil);
        
        // Setup OrderDto
        orderDto = OrderDto.builder()
                .orderId(1)
                .orderDate(LocalDateTime.now())
                .orderDesc("Test Order")
                .orderStatus("CREATED")
                .orderFee(100.0)
                .build();

        // Setup PaymentDto
        paymentDto = PaymentDto.builder()
                .paymentId(1)
                .isPayed(false)
                .paymentStatus(PaymentStatus.NOT_STARTED)
                .orderDto(orderDto)
                .build();

        // Setup Collection Response
        Collection<PaymentDto> payments = Arrays.asList(paymentDto);
        collectionResponse = PaymentPaymentServiceDtoCollectionResponse.builder()
                .collection(payments)
                .build();
    }

    @Test
    @DisplayName("Should find all payments successfully")
    void findAll_ShouldReturnAllPayments_WhenCalled() {
        // Given
        ResponseEntity<PaymentPaymentServiceDtoCollectionResponse> serviceResponse = 
            new ResponseEntity<>(collectionResponse, HttpStatus.OK);
        when(paymentClientService.findAll()).thenReturn(serviceResponse);

        // When
        ResponseEntity<PaymentPaymentServiceDtoCollectionResponse> result = paymentController.findAll();

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(collectionResponse, result.getBody());
        verify(paymentClientService, times(1)).findAll();
    }

    @Test
    @DisplayName("Should find payment by ID successfully when user is authorized")
    void findById_ShouldReturnPayment_WhenUserIsAuthorized() {
        // Given
        String paymentId = "1";
        String userId = "1";
        ResponseEntity<PaymentDto> serviceResponse = new ResponseEntity<>(paymentDto, HttpStatus.OK);
        
        when(authUtil.getOwner(paymentId, ResourceType.PAYMENTS)).thenReturn(userId);
        doNothing().when(authUtil).canActivate(request, userId, userDetails);
        when(paymentClientService.findById(paymentId)).thenReturn(serviceResponse);

        // When
        ResponseEntity<PaymentDto> result = paymentController.findById(paymentId, request, userDetails);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(paymentDto, result.getBody());
        verify(authUtil, times(1)).getOwner(paymentId, ResourceType.PAYMENTS);
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(paymentClientService, times(1)).findById(paymentId);
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when user is not authorized to find payment by ID")
    void findById_ShouldThrowUnauthorizedException_WhenUserIsNotAuthorized() {
        // Given
        String paymentId = "1";
        String userId = "1";
        
        when(authUtil.getOwner(paymentId, ResourceType.PAYMENTS)).thenReturn(userId);
        doThrow(new UnauthorizedException("You can access to resources of your own"))
            .when(authUtil).canActivate(request, userId, userDetails);

        // When & Then
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            paymentController.findById(paymentId, request, userDetails);
        });
        
        assertEquals("You can access to resources of your own", exception.getMessage());
        verify(authUtil, times(1)).getOwner(paymentId, ResourceType.PAYMENTS);
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(paymentClientService, times(0)).findById(anyString());
    }

    @Test
    @DisplayName("Should save payment successfully when user is authorized")
    void save_ShouldReturnSavedPayment_WhenUserIsAuthorized() {
        // Given
        String orderId = "1";
        String userId = "1";
        ResponseEntity<PaymentDto> serviceResponse = new ResponseEntity<>(paymentDto, HttpStatus.OK);
        
        when(authUtil.getOwner(orderId, ResourceType.ORDERS)).thenReturn(userId);
        doNothing().when(authUtil).canActivate(request, userId, userDetails);
        when(paymentClientService.save(paymentDto)).thenReturn(serviceResponse);

        // When
        ResponseEntity<PaymentDto> result = paymentController.save(paymentDto, request, userDetails);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(paymentDto, result.getBody());
        verify(authUtil, times(1)).getOwner(orderId, ResourceType.ORDERS);
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(paymentClientService, times(1)).save(paymentDto);
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when user is not authorized to save payment")
    void save_ShouldThrowUnauthorizedException_WhenUserIsNotAuthorized() {
        // Given
        String orderId = "1";
        String userId = "1";
        
        when(authUtil.getOwner(orderId, ResourceType.ORDERS)).thenReturn(userId);
        doThrow(new UnauthorizedException("You can access to resources of your own"))
            .when(authUtil).canActivate(request, userId, userDetails);

        // When & Then
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            paymentController.save(paymentDto, request, userDetails);
        });
        
        assertEquals("You can access to resources of your own", exception.getMessage());
        verify(authUtil, times(1)).getOwner(orderId, ResourceType.ORDERS);
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(paymentClientService, times(0)).save(any(PaymentDto.class));
    }

    @Test
    @DisplayName("Should update payment status successfully")
    void updateStatus_ShouldReturnUpdatedPayment_WhenCalled() {
        // Given
        String paymentId = "1";
        ResponseEntity<PaymentDto> serviceResponse = new ResponseEntity<>(paymentDto, HttpStatus.OK);
        
        when(paymentClientService.updateStatus(paymentId)).thenReturn(serviceResponse);

        // When
        ResponseEntity<PaymentDto> result = paymentController.updateStatus(paymentId);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(paymentDto, result.getBody());
        verify(paymentClientService, times(1)).updateStatus(paymentId);
    }

    @Test
    @DisplayName("Should delete payment by ID successfully when user is authorized")
    void deleteById_ShouldReturnTrue_WhenUserIsAuthorizedAndDeletionSuccessful() {
        // Given
        String paymentId = "1";
        String userId = "1";
        ResponseEntity<Boolean> serviceResponse = new ResponseEntity<>(true, HttpStatus.OK);
        
        when(authUtil.getOwner(paymentId, ResourceType.PAYMENTS)).thenReturn(userId);
        doNothing().when(authUtil).canActivate(request, userId, userDetails);
        when(paymentClientService.deleteById(paymentId)).thenReturn(serviceResponse);

        // When
        ResponseEntity<Boolean> result = paymentController.deleteById(paymentId, request, userDetails);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(true, result.getBody());
        verify(authUtil, times(1)).getOwner(paymentId, ResourceType.PAYMENTS);
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(paymentClientService, times(1)).deleteById(paymentId);
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when user is not authorized to delete payment")
    void deleteById_ShouldThrowUnauthorizedException_WhenUserIsNotAuthorized() {
        // Given
        String paymentId = "1";
        String userId = "1";
        
        when(authUtil.getOwner(paymentId, ResourceType.PAYMENTS)).thenReturn(userId);
        doThrow(new UnauthorizedException("You can access to resources of your own"))
            .when(authUtil).canActivate(request, userId, userDetails);

        // When & Then
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            paymentController.deleteById(paymentId, request, userDetails);
        });
        
        assertEquals("You can access to resources of your own", exception.getMessage());
        verify(authUtil, times(1)).getOwner(paymentId, ResourceType.PAYMENTS);
        verify(authUtil, times(1)).canActivate(request, userId, userDetails);
        verify(paymentClientService, times(0)).deleteById(anyString());
    }

    @Test
    @DisplayName("Should handle null userId from getOwner method")
    void findById_ShouldHandleNullUserId_WhenGetOwnerReturnsNull() {
        // Given
        String paymentId = "1";
        ResponseEntity<PaymentDto> serviceResponse = new ResponseEntity<>(paymentDto, HttpStatus.OK);
        
        when(authUtil.getOwner(paymentId, ResourceType.PAYMENTS)).thenReturn(null);
        doNothing().when(authUtil).canActivate(request, null, userDetails);
        when(paymentClientService.findById(paymentId)).thenReturn(serviceResponse);

        // When
        ResponseEntity<PaymentDto> result = paymentController.findById(paymentId, request, userDetails);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(paymentDto, result.getBody());
        verify(authUtil, times(1)).getOwner(paymentId, ResourceType.PAYMENTS);
        verify(authUtil, times(1)).canActivate(request, null, userDetails);
        verify(paymentClientService, times(1)).findById(paymentId);
    }

    @Test
    @DisplayName("Should handle service returning null response body")
    void findAll_ShouldHandleNullResponseBody_WhenServiceReturnsNullBody() {
        // Given
        ResponseEntity<PaymentPaymentServiceDtoCollectionResponse> serviceResponse = 
            new ResponseEntity<>(null, HttpStatus.OK);
        when(paymentClientService.findAll()).thenReturn(serviceResponse);

        // When
        ResponseEntity<PaymentPaymentServiceDtoCollectionResponse> result = paymentController.findAll();

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(null, result.getBody());
        verify(paymentClientService, times(1)).findAll();
    }

    @Test
    @DisplayName("Should handle empty payment collection")
    void findAll_ShouldHandleEmptyCollection_WhenNoPaymentsExist() {
        // Given
        Collection<PaymentDto> emptyPayments = Arrays.asList();
        PaymentPaymentServiceDtoCollectionResponse emptyResponse = PaymentPaymentServiceDtoCollectionResponse.builder()
                .collection(emptyPayments)
                .build();
        ResponseEntity<PaymentPaymentServiceDtoCollectionResponse> serviceResponse = 
            new ResponseEntity<>(emptyResponse, HttpStatus.OK);
        when(paymentClientService.findAll()).thenReturn(serviceResponse);

        // When
        ResponseEntity<PaymentPaymentServiceDtoCollectionResponse> result = paymentController.findAll();

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(0, result.getBody().getCollection().size());
        verify(paymentClientService, times(1)).findAll();
    }

    @Test
    @DisplayName("Should handle payment with null order")
    void save_ShouldHandlePaymentWithNullOrder_WhenOrderDtoIsNull() {
        // Given
        PaymentDto paymentWithNullOrder = PaymentDto.builder()
                .paymentId(1)
                .isPayed(false)
                .paymentStatus(PaymentStatus.NOT_STARTED)
                .orderDto(null)
                .build();

        // When & Then
        assertThrows(NullPointerException.class, () -> {
            paymentController.save(paymentWithNullOrder, request, userDetails);
        });

        verify(paymentClientService, times(0)).save(any(PaymentDto.class));
    }

    @Test
    @DisplayName("Should handle different payment statuses")
    void updateStatus_ShouldHandleDifferentStatuses_WhenCalled() {
        // Given
        String paymentId = "1";
        PaymentDto completedPayment = PaymentDto.builder()
                .paymentId(1)
                .isPayed(true)
                .paymentStatus(PaymentStatus.COMPLETED)
                .orderDto(orderDto)
                .build();
        ResponseEntity<PaymentDto> serviceResponse = new ResponseEntity<>(completedPayment, HttpStatus.OK);
        
        when(paymentClientService.updateStatus(paymentId)).thenReturn(serviceResponse);

        // When
        ResponseEntity<PaymentDto> result = paymentController.updateStatus(paymentId);

        // Then
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(completedPayment, result.getBody());
        assertEquals(PaymentStatus.COMPLETED, result.getBody().getPaymentStatus());
        assertEquals(true, result.getBody().getIsPayed());
        verify(paymentClientService, times(1)).updateStatus(paymentId);
    }
}