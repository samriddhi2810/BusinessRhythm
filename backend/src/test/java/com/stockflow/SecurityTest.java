package com.stockflow;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={
 "spring.datasource.url=jdbc:h2:mem:test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
 "spring.datasource.username=sa",
 "spring.datasource.password=",
 "spring.datasource.driver-class-name=org.h2.Driver",
 "stockflow.username=test-owner",
 "stockflow.password=test-owner-password"
})
@AutoConfigureMockMvc
class SecurityTest {
 @Autowired MockMvc mvc;

 String basic(String password) {
  return "Basic " + Base64.getEncoder().encodeToString(("test-owner:" + password).getBytes(StandardCharsets.UTF_8));
 }

 @Test void correctCredentialsReturnSignedInUser() throws Exception {
  mvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION,basic("test-owner-password")))
   .andExpect(status().isOk())
   .andExpect(jsonPath("$.username").value("test-owner"));
 }

 @Test void incorrectCredentialsAreRejected() throws Exception {
  mvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION,basic("wrong-password")))
   .andExpect(status().isUnauthorized());
 }

 @Test void missingCredentialsAreRejected() throws Exception {
  mvc.perform(get("/api/me"))
   .andExpect(status().isUnauthorized())
   .andExpect(header().exists(HttpHeaders.WWW_AUTHENTICATE));
 }
}
