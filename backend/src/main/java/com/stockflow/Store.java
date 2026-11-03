package com.stockflow;

import java.math.BigDecimal;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class Store {
 final JdbcTemplate db;
 public Store(JdbcTemplate db) { this.db = db; }
 public record Product(@NotBlank @Size(max=120) String name, @NotNull @DecimalMin("0") @Digits(integer=10,fraction=2) BigDecimal price, @Min(0) int stock, @Min(0) int minimumStock) {}
 public record Item(@NotNull @Positive Long productId, @Min(1) @Max(1000000) int quantity) {}
 public record NewOrder(@NotBlank @Size(max=120) String customerName, @NotBlank @Size(max=160) String contact, @NotEmpty @Size(max=100) List<@NotNull @Valid Item> items) {}
 static ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
 Map<String,Object> product(long id, boolean lock) {
  var rows = db.queryForList("SELECT * FROM products WHERE id=?" + (lock ? " FOR UPDATE" : ""), id);
  if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Product not found");
  return rows.getFirst();
 }
 long insert(String sql, Object... values) {
  KeyHolder key = new GeneratedKeyHolder();
  db.update(c -> { var ps = c.prepareStatement(sql, new String[]{"id"}); for(int i=0;i<values.length;i++) ps.setObject(i+1,values[i]); return ps; }, key);
  return Objects.requireNonNull(key.getKey()).longValue();
 }
 public List<Map<String,Object>> products() { return db.queryForList("SELECT * FROM products WHERE active=TRUE ORDER BY name"); }
 @Transactional public long save(Long id, Product p) {
  if(id==null) return insert("INSERT INTO products(name,price,stock,minimum_stock) VALUES(?,?,?,?)",p.name().trim(),p.price(),p.stock(),p.minimumStock());
  var old=product(id,true);
  if(!Boolean.TRUE.equals(old.get("active"))) throw bad("Product is archived");
  db.update("UPDATE products SET name=?,price=?,stock=?,minimum_stock=? WHERE id=?",p.name().trim(),p.price(),p.stock(),p.minimumStock(),id); return id;
 }
 @Transactional public void delete(long id) { product(id,true); db.update("UPDATE products SET active=FALSE WHERE id=?",id); }
 @Transactional public long create(NewOrder input) {
  var items=new TreeMap<Long,Integer>();
  for(var item:input.items()) if(items.putIfAbsent(item.productId(),item.quantity())!=null) throw bad("Each product may appear only once");
  var prices=new HashMap<Long,BigDecimal>(); BigDecimal total=BigDecimal.ZERO;
  for(var item:items.entrySet()) {
   var p=product(item.getKey(),true);
   if(!Boolean.TRUE.equals(p.get("active"))) throw bad("Product is archived");
   var price=(BigDecimal)p.get("price"); prices.put(item.getKey(),price); total=total.add(price.multiply(BigDecimal.valueOf(item.getValue())));
  }
  if(total.compareTo(new BigDecimal("999999999999.99"))>0) throw bad("Order total is too large");
  long customer=insert("INSERT INTO customers(name,contact) VALUES(?,?)",input.customerName().trim(),input.contact().trim());
  long id=insert("INSERT INTO orders(customer_id,total) VALUES(?,?)",customer,total);
  for(var item:items.entrySet()) db.update("INSERT INTO order_items(order_id,product_id,quantity,unit_price) VALUES(?,?,?,?)",id,item.getKey(),item.getValue(),prices.get(item.getKey()));
  return id;
 }
 public List<Map<String,Object>> orders() { return db.queryForList("SELECT o.*,c.name AS customer_name,c.contact FROM orders o JOIN customers c ON c.id=o.customer_id ORDER BY o.id DESC"); }
 public Map<String,Object> detail(long id) {
  var rows=db.queryForList("SELECT o.*,c.name AS customer_name,c.contact FROM orders o JOIN customers c ON c.id=o.customer_id WHERE o.id=?",id);
  if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Order not found");
  var result=rows.getFirst(); result.put("items",db.queryForList("SELECT i.*,p.name FROM order_items i JOIN products p ON p.id=i.product_id WHERE i.order_id=? ORDER BY i.product_id",id)); return result;
 }
 @Transactional public void transition(long id,String next) {
  var rows=db.queryForList("SELECT * FROM orders WHERE id=? FOR UPDATE",id);
  if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Order not found");
  String current=(String)rows.getFirst().get("status");
  var allowed=Map.of("Pending",Set.of("Confirmed","Cancelled"),"Confirmed",Set.of("Packed","Cancelled"),"Packed",Set.of("Completed","Cancelled"),"Completed",Set.<String>of(),"Cancelled",Set.<String>of());
  if(!allowed.get(current).contains(next)) throw bad("Invalid order transition: "+current+" to "+next);
  var items=db.queryForList("SELECT * FROM order_items WHERE order_id=? ORDER BY product_id",id);
  for(var item:items) {
   long pid=((Number)item.get("product_id")).longValue(); int quantity=((Number)item.get("quantity")).intValue();
   var p=product(pid,true);
   if(next.equals("Confirmed")) {
    if(!Boolean.TRUE.equals(p.get("active"))) throw bad("An ordered product is archived");
    if(db.update("UPDATE products SET stock=stock-? WHERE id=? AND stock>=?",quantity,pid,quantity)!=1) throw bad("Insufficient stock for "+p.get("name"));
   } else if(next.equals("Cancelled") && !current.equals("Pending")) {
    if((long)((Number)p.get("stock")).intValue()+quantity>Integer.MAX_VALUE) throw bad("Stock limit exceeded");
    db.update("UPDATE products SET stock=stock+? WHERE id=?",quantity,pid);
   }
  }
  db.update("UPDATE orders SET status=? WHERE id=?",next,id);
 }
 public Map<String,Object> report() {
  var r=new LinkedHashMap<String,Object>();
  r.put("products",db.queryForObject("SELECT COUNT(*) FROM products WHERE active=TRUE",Long.class));
  r.put("openOrders",db.queryForObject("SELECT COUNT(*) FROM orders WHERE status IN ('Pending','Confirmed','Packed')",Long.class));
  r.put("lowStock",db.queryForList("SELECT * FROM products WHERE active=TRUE AND stock<=minimum_stock ORDER BY stock,name"));
  r.put("sales",db.queryForObject("SELECT COALESCE(SUM(total),0) FROM orders WHERE status='Completed'",BigDecimal.class));
  r.put("dailySales",db.queryForList("SELECT CAST(order_date AS DATE) AS `day`,COUNT(*) AS orders,SUM(total) AS sales FROM orders WHERE status='Completed' GROUP BY CAST(order_date AS DATE) ORDER BY `day` DESC LIMIT 30"));
  return r;
 }
}
