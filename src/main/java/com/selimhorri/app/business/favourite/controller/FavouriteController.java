package com.selimhorri.app.business.favourite.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.selimhorri.app.business.favourite.model.FavouriteDto;
import com.selimhorri.app.business.favourite.model.FavouriteId;
import com.selimhorri.app.business.favourite.model.response.FavouriteFavouriteServiceCollectionDtoResponse;
import com.selimhorri.app.business.favourite.service.FavouriteClientService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/favourites")
@RequiredArgsConstructor
public class FavouriteController {
	
	private final FavouriteClientService favouriteClientService;
	
	@GetMapping
	public ResponseEntity<FavouriteFavouriteServiceCollectionDtoResponse> findAll() {
		return ResponseEntity.ok(this.favouriteClientService.findAll().getBody());
	}
	
	@GetMapping("/{userId}/{productId}")
	public ResponseEntity<FavouriteDto> findById(
			@PathVariable("userId") final String userId, 
			@PathVariable("productId") final String productId) {
		return ResponseEntity.ok(this.favouriteClientService.findById(userId, productId).getBody());
	}
	
	@PostMapping
	public ResponseEntity<FavouriteDto> save(@RequestBody final FavouriteDto favouriteDto) {
		return ResponseEntity.ok(this.favouriteClientService.save(favouriteDto).getBody());
	}
	
	@PutMapping
	public ResponseEntity<FavouriteDto> update(@RequestBody final FavouriteDto favouriteDto) {
		return ResponseEntity.ok(this.favouriteClientService.update(favouriteDto).getBody());
	}
	
	@DeleteMapping("/{userId}/{productId}")
	public ResponseEntity<Boolean> deleteById(
			@PathVariable("userId") final String userId, 
			@PathVariable("productId") final String productId) {
		this.favouriteClientService.deleteById(userId, productId).getBody();
		return ResponseEntity.ok(true);
	}
	
}










