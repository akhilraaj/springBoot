package com.netprise.sample.controller;

import java.io.IOException;
import java.util.Date;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.NumberUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
public class MainController {

	CamelContext context = new DefaultCamelContext();
	@Autowired
	private DataSource dataSource;

	final CamelContext camelContext = context;

	@PostConstruct
	public void initializeCamelContext() {
		final org.apache.camel.impl.SimpleRegistry registry = new org.apache.camel.impl.SimpleRegistry();
		final org.apache.camel.impl.CompositeRegistry compositeRegistry = new org.apache.camel.impl.CompositeRegistry();
		compositeRegistry.addRegistry(camelContext.getRegistry());
		compositeRegistry.addRegistry(registry);
		((org.apache.camel.impl.DefaultCamelContext) camelContext).setRegistry(compositeRegistry);
		registry.put("dataSource", dataSource);
		try {
			context.addRoutes(new RouteBuilder() {
				@Override
				public void configure() throws Exception {

					from("direct:in").to("jdbc:dataSource");
				}
			});
			context.start();
		} catch (Exception e) {
			e.printStackTrace();

		}
	}

	@RequestMapping("/convert")
	public String convert(String currency, Model model) throws Exception {

		return "search";

	}

	@RequestMapping("/process")
	@ResponseBody
	public String convertCurrency(String amt) {
		System.out.println("Amount : " + String.valueOf(amt));
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String name = auth.getName();
		String query = "INSERT INTO `test`.`search` (`currency`, `username`,`created_time`) VALUES ('52', '" + name
				+ "',NOW())";
		ProducerTemplate template = context.createProducerTemplate();
		template.sendBody("direct:in", query);
		final String uri = "http://api.fixer.io/latest?base=USD";

		RestTemplate restTemplate = new RestTemplate();
		String result = restTemplate.getForObject(uri, String.class);

		
		ObjectMapper mapper = new ObjectMapper();
		try {
			Integer amount = NumberUtils.parseNumber(amt, Integer.class);
			JsonNode actualObj = mapper.readTree(result);
			JsonNode rates = actualObj.get("rates");
			System.out.println(rates.get("INR").asText());
			float usd = amount / NumberUtils.parseNumber(rates.get("INR").asText(), Float.class);
			System.out.println(usd);
			return String.valueOf(usd);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (NumberFormatException e) {
			return "Please enter a valid number";
		}
		return "";

	}

}
