package au.com.ing.microservice.notificationproducer.consumer;

//Copyright (c) Microsoft Corporation. All rights reserved.

//Licensed under the MIT License.

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Properties;

import org.apache.kafka.clients.consumer.CommitFailedException;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.TopicAuthorizationException;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

public class TestConsumerThread implements Runnable {

 

    private boolean isPrimaryKeyInUse;

    private final String topic;

    private String primarySharedAccessKey;

    private String secondarySharedAccesskey;

    private String jaasConfigValue;

    private final String jaasConfigProperty;

 

    // Each consumer needs a unique client ID per thread

    private static int id = 0;

 

    public TestConsumerThread(final String topic) {

 

        this.topic = topic;

        this.primarySharedAccessKey = "u1kCGnfYCGSj2Tzy6bDotcV18S4ZT8UmxfGJanbMtDU=";

        this.secondarySharedAccesskey = "JWasKBRjic0GsrUM6/UYS1aKy+miXtXvAlPjI0f5+y0=";

        this.jaasConfigValue = "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$ConnectionString\""
                + " password=\"Endpoint=sb://jaifirstnamespace.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=_SAK_;EntityPath=secondeventhub\";";

        this.jaasConfigProperty = "sasl.jaas.config";

        this.isPrimaryKeyInUse = false;

    }

 

    public void run() throws RuntimeException {

        do {

                final Consumer<Long, String> consumer = createConsumer();

                System.out.println("Polling");


                try {

                    while (true) {

                        final ConsumerRecords<Long, String> consumerRecords = consumer.poll(1000);

                        for (ConsumerRecord<Long, String> cr : consumerRecords) {

                            System.out.printf("Consumer Record:(%d, %s, %d, %d)\n", cr.key(), cr.value(), cr.partition(),

                                    cr.offset());

                        }

                        consumer.commitAsync();

                    }

                } catch (TopicAuthorizationException e) {


                    System.out.println("TopicAuthorizationException He he: " + e);

                } catch (CommitFailedException e) {

                    System.out.println("CommitFailedException: " + e);

                } finally {

                    consumer.close();

                }

        } while (true);

    }

    private Consumer<Long, String> createConsumer() throws RuntimeException {


        // Create the consumer using properties.
        try (final Consumer<Long, String> consumer = new KafkaConsumer<>(createProperties())) {

            // Subscribe to the topic.

            consumer.subscribe(Collections.singletonList(topic));

            return consumer;

        }
    }



    private Properties createProperties() throws RuntimeException {

        try (FileReader fr = new FileReader("src/main/resources/consumer.config")) {

            final Properties properties = new Properties();
            synchronized (TestConsumerThread.class) {

                properties.put(ConsumerConfig.CLIENT_ID_CONFIG, "KafkaExampleConsumer#" + id);

                id++;

            }

            properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class.getName());

            properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());


            // Get remaining properties from config file

            properties.load(fr);

            this.isPrimaryKeyInUse = !this.isPrimaryKeyInUse;

            String secretKey = this.isPrimaryKeyInUse ? this.primarySharedAccessKey

                    : this.secondarySharedAccesskey;

            System.out.println("secret key: " + secretKey);


            System.out.println("The Secret in use is: " + (this.isPrimaryKeyInUse ? "PRIMARY_SHARED_ACCESS_KEY" : "SECONDRY_SHARED_ACCESS_KEY"));

            properties.put(this.jaasConfigProperty, this.jaasConfigValue.replaceAll("_SAK_", secretKey));
            return  properties;
        } catch (IOException e) {

            System.out.println("IOException: " + e);
            throw new  RuntimeException();
        }

    }
}

