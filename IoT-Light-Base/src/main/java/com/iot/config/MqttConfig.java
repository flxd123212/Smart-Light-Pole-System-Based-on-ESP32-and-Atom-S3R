package com.iot.config;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.iot.service.IIotMqttMessageService;

@Configuration
public class MqttConfig {

  private static final Logger log = LoggerFactory.getLogger(MqttConfig.class);

  @Value("${spring.mqtt.broker}")
  private String broker;

  @Value("${spring.mqtt.client-id}")
  private String clientId;

  @Value("${spring.mqtt.username}")
  private String username;

  @Value("${spring.mqtt.password}")
  private String password;

  @Value("${spring.mqtt.topics}")
  private String topics;

  @Value("${spring.mqtt.topics-legacy:lamp/#}")
  private String topicsLegacy;

  @Value("${spring.mqtt.qos}")
  private int qos;

  @Autowired
  private IIotMqttMessageService iotMqttMessageService;

  @Bean
  public MqttClient mqttClient() throws MqttException {
    MqttClient client = new MqttClient(broker, clientId, new MemoryPersistence());

    MqttConnectOptions options = new MqttConnectOptions();
    options.setCleanSession(true);
    options.setAutomaticReconnect(true);
    if (username != null && !username.isEmpty()) {
      options.setUserName(username);
    }
    if (password != null && !password.isEmpty()) {
      options.setPassword(password.toCharArray());
    }

    client.setCallback(new MqttCallbackExtended() {
      @Override
      public void connectComplete(boolean reconnect, String serverURI) {
        log.info("====== MQTT 连接完成: {} ======", serverURI);
        try {
          client.subscribe(topics, qos);
          log.info("====== 已订阅主题: {} ======", topics);
          client.subscribe(topicsLegacy, qos);
          log.info("====== 已订阅旧版主题: {} ======", topicsLegacy);
        } catch (MqttException e) {
          log.error("订阅失败", e);
        }
      }

      @Override
      public void connectionLost(Throwable cause) {
        log.warn("====== MQTT 连接断开 ======", cause);
      }

      @Override
      public void messageArrived(String topic, MqttMessage message){
        try{
          String payload = new String(message.getPayload());
          iotMqttMessageService.handleDeviceMessage(topic, payload);
        }catch (Exception e){
          log.error("MQTT消息接收异常, topic={}", topic, e);
        }
      }

      @Override
      public void deliveryComplete(IMqttDeliveryToken token) {
      }
    });

    client.connect(options);
    log.info("====== MQTT 连接成功: {} ======", broker);

    return client;
  }
}
