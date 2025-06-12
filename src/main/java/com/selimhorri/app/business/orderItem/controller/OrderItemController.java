package com.selimhorri.app.business.orderItem.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.selimhorri.app.business.orderItem.model.OrderItemDto;
import com.selimhorri.app.business.orderItem.model.OrderItemId;
import com.selimhorri.app.business.orderItem.model.response.OrderItemOrderItemServiceDtoCollectionResponse;
import com.selimhorri.app.business.orderItem.service.OrderItemClientService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/shippings")
@RequiredArgsConstructor
public class OrderItemController {
	
	private final OrderItemClientService orderItemClientService;
	
	@GetMapping
	public ResponseEntity<OrderItemOrderItemServiceDtoCollectionResponse> findAll() {
		return ResponseEntity.ok(this.orderItemClientService.findAll().getBody());
	}
	
	@GetMapping("/{orderId}")
	public ResponseEntity<OrderItemDto> findById(
			@PathVariable("orderId") final String orderId) {
		return ResponseEntity.ok(this.orderItemClientService.findById(orderId).getBody());
	}
	
	
	@PostMapping
	public ResponseEntity<OrderItemDto> save(@RequestBody final OrderItemDto orderItemDto) {
		return ResponseEntity.ok(this.orderItemClientService.save(orderItemDto).getBody());
	}

	@DeleteMapping("/{orderId}")
	public ResponseEntity<Boolean> deleteById(
			@PathVariable("orderId") final String orderId) {
		this.orderItemClientService.deleteById(orderId).getBody();
		return ResponseEntity.ok(true);
	}	
	
}










