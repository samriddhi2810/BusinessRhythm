package com.stockflow;

import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;

@RestController
@RequestMapping("/api")
public class Api {
 final Store store; public Api(Store store) { this.store=store; }
 @GetMapping("/me") public Map<String,String> me(java.security.Principal principal) { return Map.of("username",principal.getName()); }
 @GetMapping("/products") public Object products() { return store.products(); }
 @PostMapping("/products") public Object create(@Valid @RequestBody Store.Product p) { return Map.of("id",store.save(null,p)); }
 @PutMapping("/products/{id}") public Object update(@PathVariable long id,@Valid @RequestBody Store.Product p) { return Map.of("id",store.save(id,p)); }
 @DeleteMapping("/products/{id}") public void delete(@PathVariable long id) { store.delete(id); }
 @GetMapping("/orders") public Object orders() { return store.orders(); }
 @GetMapping("/orders/{id}") public Object order(@PathVariable long id) { return store.detail(id); }
 @PostMapping("/orders") public Object create(@Valid @RequestBody Store.NewOrder o) { return Map.of("id",store.create(o)); }
 record Status(@NotBlank String status) {}
 @PatchMapping("/orders/{id}/status") public void status(@PathVariable long id,@Valid @RequestBody Status s) { store.transition(id,s.status()); }
 @GetMapping("/reports") public Object reports() { return store.report(); }
 @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<?> conflict(ResponseStatusException e) { return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",Objects.requireNonNullElse(e.getReason(),"Request failed"))); }
 @ExceptionHandler(MethodArgumentNotValidException.class) public ResponseEntity<?> invalid(MethodArgumentNotValidException e) { return ResponseEntity.badRequest().body(Map.of("message",e.getBindingResult().getFieldErrors().stream().map(f->f.getField()+": "+f.getDefaultMessage()).toList().toString())); }
}
