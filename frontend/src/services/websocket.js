import { Client } from "@stomp/stompjs";

let stompClient = null;

export const connectWebSocket = (onMessageReceived) => {
  stompClient = new Client({
    brokerURL: "ws://localhost:8080/ws",

    reconnectDelay: 5000,

    onConnect: () => {
      console.log("✅ Connected");

      stompClient.subscribe("/topic/messages", (message) => {
        onMessageReceived(JSON.parse(message.body));
      });
    },

    onStompError: (frame) => {
      console.error("STOMP Error:", frame);
    },

    onWebSocketError: (error) => {
      console.error("WebSocket Error:", error);
    },
  });

  stompClient.activate();
};

export const sendMessage = (message) => {
  if (stompClient && stompClient.connected) {
    stompClient.publish({
      destination: "/app/send",
      body: JSON.stringify(message),
    });
  }
};

export const disconnect = () => {
  if (stompClient) {
    stompClient.deactivate();
  }
};