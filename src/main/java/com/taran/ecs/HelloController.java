package com.taran.ecs;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

	@GetMapping("/hello")
	public Map<String, String> sayHello() {
		Map<String, String> response = new HashMap<>();
		response.put("message", "Hello from Deployment!");
		return response;
	}
	@GetMapping("/welcome")
	public Map<String, String> welcome() {
		Map<String, String> response = new HashMap<>();
		response.put("message", "welcome to the api again!");
		response.put("status", "success");
		return response;
	}
}
