package com.fun.projects.cb;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.MessageAttributeValue;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;

public class CrateAndBarrelPriceTracker implements RequestHandler<Object, Boolean> {

	private static final String SKU = <Enter Your SKU>; 
	private static final double THRESHOLD = <Enter Your Price>;
	private static final String PHONE_NUMBER = <Enter Your Phone>;
  
	private static final String ADDRESS = "http://www.arcusallc.com/crateitemapi3.php";
	private static final String PARAMS = "sku-number=" + SKU;

	private static final String PATTERN = "Our Price Range\\s*:\\s*\\$\\d*\\.\\d*";

	private AmazonSNS snsClient = AmazonSNSClientBuilder.defaultClient();

	@Override
	public Boolean handleRequest(Object input, Context context) {

		try {
			String data = getData();
			double price = getPrice(data);
			System.out.println("Current price: $" + price);
			if (price <= THRESHOLD) {
				sendSMS(price);
			}
		} catch (Exception ex) {
			System.err.println("Error: " + ex.getMessage());
		}

		return true;
	}

	private void sendSMS(double price) {

		String message = SKU + ": $" + price;
		PublishResult result = snsClient.publish(new PublishRequest().withMessage(message).withPhoneNumber(PHONE_NUMBER)
				.withMessageAttributes(new HashMap<String, MessageAttributeValue>()));
		System.out.println("SMS Result: " + result);
	}

	private double getPrice(String data) {

		Pattern pattern = Pattern.compile(PATTERN);
		Matcher matcher = pattern.matcher(data);
		if (matcher.find()) {
			String matched = matcher.group();
			String price = matched.substring(matched.indexOf("$") + 1).trim();
			return Double.parseDouble(price);
		} else {
			return 0.0;
		}
	}

	private String getData() throws Exception {

		URL url = new URL(ADDRESS);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setDoOutput(true);
		conn.setInstanceFollowRedirects(false);
		conn.setUseCaches(false);
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		conn.setRequestProperty("Content-Length", Integer.toString(PARAMS.getBytes().length));

		try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
			wr.write(PARAMS.getBytes());
		}

		StringBuffer response = new StringBuffer();
		try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
		}

		return response.toString();
	}

}
