package com.stockflow;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","spring.datasource.username=sa","spring.datasource.password=","spring.datasource.driver-class-name=org.h2.Driver"})
@Transactional
class StoreTest {
 @Autowired Store store;
 long product(int stock) { return store.save(null,new Store.Product("Tea",new BigDecimal("12.50"),stock,2)); }
 long order(long product,int qty) { return store.create(new Store.NewOrder("Customer","123456",List.of(new Store.Item(product,qty)))); }
 int stock(long id) { return ((Number)store.product(id,false).get("stock")).intValue(); }
 @Test void confirmationDeductsAndCancellationRestoresExactlyOnce() {
  long p=product(10),o=order(p,4); assertEquals(10,stock(p));
  store.transition(o,"Confirmed"); assertEquals(6,stock(p));
  assertThrows(ResponseStatusException.class,()->store.transition(o,"Confirmed")); assertEquals(6,stock(p));
  store.transition(o,"Packed"); store.transition(o,"Cancelled"); assertEquals(10,stock(p));
  assertThrows(ResponseStatusException.class,()->store.transition(o,"Cancelled")); assertEquals(10,stock(p));
 }
 @Test void insufficientStockPreventsConfirmation() {
  long p=product(2),o=order(p,3);
  assertThrows(ResponseStatusException.class,()->store.transition(o,"Confirmed")); assertEquals(2,stock(p));
  assertEquals("Pending",store.detail(o).get("status"));
 }
 @Test void completedOrderIsFinalAndPricesAreSnapshots() {
  long p=product(5),o=order(p,2);
  store.save(p,new Store.Product("Tea",new BigDecimal("99.00"),5,2));
  assertEquals(0,new BigDecimal("25.00").compareTo((BigDecimal)store.detail(o).get("total")));
  store.transition(o,"Confirmed");store.transition(o,"Packed");store.transition(o,"Completed");
  assertThrows(ResponseStatusException.class,()->store.transition(o,"Cancelled")); assertEquals(3,stock(p));
  assertEquals(0,new BigDecimal("25.00").compareTo((BigDecimal)store.report().get("sales")));
 }
 @Test void pendingCancellationDoesNotChangeStock() {long p=product(4),o=order(p,2);store.transition(o,"Cancelled");assertEquals(4,stock(p));}
 @Test void lowStockIncludesThresholdAndArchivingPreservesHistory() {
  long p=product(2),o=order(p,1);
  assertFalse(((List<?>)store.report().get("lowStock")).isEmpty());
  store.delete(p);assertNotNull(store.detail(o));assertTrue(store.products().isEmpty());
  assertThrows(ResponseStatusException.class,()->store.transition(o,"Confirmed"));
 }
}
