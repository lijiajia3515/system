package com.hfhk.system.service;

import io.minio.MinioClient;
import io.minio.PostPolicy;
import io.minio.errors.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.util.Map;

@SpringBootApplication
public class SystemServiceApp {

	public static void main(String[] args) throws IOException, InvalidKeyException, InvalidResponseException, InsufficientDataException, NoSuchAlgorithmException, ServerException, InternalException, XmlParserException, ErrorResponseException {
		MinioClient client = MinioClient.builder().credentials("root", "Hfhk.1320.").endpoint("http://192.168.30.22:20000").build();
		PostPolicy dev = new PostPolicy("dev", ZonedDateTime.now().plusDays(2));
		dev.addEqualsCondition("key", "/**");
		Map<String, String> presignedPostFormData = client.getPresignedPostFormData(dev);

		presignedPostFormData.forEach((k, v) -> {
			System.out.printf(String.format("[%s , %s]\n", k, v));
		});

		SpringApplication.run(SystemServiceApp.class, args);
	}

}
