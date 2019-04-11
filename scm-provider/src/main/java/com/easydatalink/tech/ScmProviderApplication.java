package com.easydatalink.tech;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * scm 提供方
 * @author Terry
 *
 */
@EnableDiscoveryClient
@SpringBootApplication
public class ScmProviderApplication {

	public static void main(String[] args) {
		SpringApplication.run(ScmProviderApplication.class, args);
	}
}