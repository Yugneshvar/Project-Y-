package backend.controller;

import backend.model.Message;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {

    @MessageMapping("/send")
    @SendTo("/topic/messages")
    public Message sendMessage(Message message) {

        System.out.println("Message received from: " + message.getSender());
        System.out.println("Content: " + message.getContent());

        return message;
    }
}