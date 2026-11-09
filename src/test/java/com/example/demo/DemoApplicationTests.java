package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import java.net.URI;
import java.net.http.*;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class DemoApplicationTests {
    @LocalServerPort int port;
    final ObjectMapper json = new ObjectMapper();
    HttpResponse<String> call(String method, String path, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/todos"+path))
                .header("Content-Type","application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
    @Test void lifecycle() throws Exception {
        var created=call("POST","","{\"id\":900000,\"name\":\"  Seminar  \",\"createdAt\":\"2000-01-01\"}");
        assertEquals(201, created.statusCode(),created.body());
        var t=json.readTree(created.body());
        assertEquals("Seminar",t.get("name").asString());
        assertEquals("mittel",t.get("priority").asString());
        assertFalse(t.get("createdAt").asString().startsWith("2000"));
        String id=t.get("id").toString();
        assertNotEquals("900000",id);
        for(String status : new String[]{"erledigt","offen"}) {
            var updated=call("PUT","/"+id,"{\"name\":\"Seminararbeit\",\"status\":\""+status+"\",\"priority\":\"hoch\",\"dueDate\":\"2020-01-01\"}");
            assertEquals(200,updated.statusCode());
            assertEquals(status,json.readTree(updated.body()).get("status").asString());
            assertEquals(t.get("createdAt"),json.readTree(updated.body()).get("createdAt"));
            assertNotEquals(t.get("updatedAt"),json.readTree(updated.body()).get("updatedAt"));
            boolean found=false;
            for(var persisted:json.readTree(call("GET","",null).body())) {
                if(persisted.get("id").toString().equals(id)) {
                    assertEquals(status,persisted.get("status").asString());
                    assertEquals("Seminararbeit",persisted.get("name").asString());
                    found=true;
                }
            }
            assertTrue(found);
        }
        assertEquals(204,call("DELETE","/"+id,null).statusCode());
        assertEquals(404,call("DELETE","/"+id,null).statusCode());
    }
    @Test void acceptsExactFieldLimits() throws Exception {
        var body=json.writeValueAsString(java.util.Map.of("name","x".repeat(120),"description","x".repeat(2000)));
        var response=call("POST","",body);
        assertEquals(201,response.statusCode());
        var task=json.readTree(response.body());
        assertEquals(120,task.get("name").asString().length());
        assertEquals(2000,task.get("description").asString().length());
        assertEquals(204,call("DELETE","/"+task.get("id"),null).statusCode());
    }
    @Test void rejectsInvalidInput() throws Exception {
        for(String body:new String[]{"{\"name\":\"   \"}","{\"name\":\""+"x".repeat(121)+"\"}",
                "{\"name\":\"x\",\"description\":\""+"x".repeat(2001)+"\"}",
                "{\"name\":\"x\",\"status\":\"unknown\"}","{\"name\":\"x\",\"priority\":\"unknown\"}",
                "{\"name\":\"x\",\"dueDate\":\"bad\"}"}) {
            var response=call("POST","",body);
            assertEquals(400,response.statusCode());
            assertTrue(json.readTree(response.body()).has("message"));
        }
        assertEquals(400,call("DELETE","/abc",null).statusCode());
        assertEquals(400,call("DELETE","/-1",null).statusCode());
        assertEquals(404,call("PUT","/9999999","{\"name\":\"x\"}").statusCode());
    }
}
