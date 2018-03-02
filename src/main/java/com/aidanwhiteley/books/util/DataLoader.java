package com.aidanwhiteley.books.util;

import com.aidanwhiteley.books.domain.Book;
import com.mongodb.BasicDBObject;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DataLoader {

	private static final String BOOKS_COLLECTION = "book";
	private static final String USERS_COLLECTION = "user";
	private static final Logger LOGGER = LoggerFactory.getLogger(DataLoader.class);
	private final MongoTemplate template;
	@Value("${books.reload.development.data}")
	private boolean reloadDevelopmentData;

	@Autowired
	public DataLoader(MongoTemplate mongoTemplate) {
		this.template = mongoTemplate;
	}

	/**
	 * Reload data for development and integration tests. Whether this runs or
	 * not is also controlled by the books.reload.development.data config
	 * setting.
	 * 
	 * Reads from files where each line is expected to be a valid JSON object but
	 * the whole file itself isnt a valid JSON object (hence the .data extension rather than .json).
	 * 
	 * Double "fail safe" of checking for required Spring profile being active
	 * and also a config switch setting.
	 */
	@Bean
	@Profile({ "dev", "integration", "unixtest" })
	public CommandLineRunner populateDummyData() {
		return args -> {

			if (reloadDevelopmentData) {

                List<String> jsons;

				// Clearing and loading data into books collection
				template.dropCollection(BOOKS_COLLECTION);
				ClassPathResource classPathResource = new ClassPathResource("sample_data/books.data");
				try (InputStream resource = classPathResource.getInputStream()) {
					jsons = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8)).lines()
							.collect(Collectors.toList());
				}
				jsons.stream().map(Document::parse).forEach(i -> template.insert(i, BOOKS_COLLECTION));
				LOGGER.info("****************************************************************************");
				LOGGER.info("Loaded development data for books as running with dev or integration profile");

				// Clearing and loading data into user collection
				template.dropCollection(USERS_COLLECTION);
				classPathResource = new ClassPathResource("sample_data/users.data");
				try (InputStream resource = classPathResource.getInputStream()) {
					jsons = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8)).lines()
							.collect(Collectors.toList());
				}
				jsons.stream().map(Document::parse).forEach(i -> template.insert(i, USERS_COLLECTION));
				LOGGER.info("Loaded development data for users as running with dev or integration profile");


                classPathResource = new ClassPathResource("indexes/books.data");
                try (InputStream resource = classPathResource.getInputStream()) {
                    jsons = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8)).lines()
                            .collect(Collectors.toList());
                }
                jsons.stream().map(s -> new BasicDBObject().append("$eval", s.toString())).forEach(i -> template.executeCommand(i));
                LOGGER.info("Created indexes for book collection as running with dev or integration profile");


				LOGGER.info("****************************************************************************");
			} else {
				LOGGER.info("Development data and indexes not reloaded due to config settings");
			}
		};
	}
}
