import { useEffect, useState } from "react";
import {
  connectWebSocket,
  disconnect,
  sendMessage,
} from "../services/websocket";

export default function WebSocketTest() {
  const [messages, setMessages] = useState([]);
  const [text, setText] = useState("");

  useEffect(() => {
    connectWebSocket((msg) => {
      setMessages((prev) => [...prev, msg]);
    });

    return () => disconnect();
  }, []);

  const send = () => {
    if (!text.trim()) return;

    sendMessage({
      sender: "Laptop",
      receiver: "All",
      type: "CHAT",
      content: text,
    });

    setText("");
  };

  return (
    <div style={{ padding: 30 }}>
      <h2>WebSocket Test</h2>

      <input
        value={text}
        onChange={(e) => setText(e.target.value)}
        placeholder="Type a message..."
      />

      <button onClick={send}>Send</button>

      <hr />

      {messages.map((m, index) => (
        <div key={index}>
          <b>{m.sender}</b> : {m.content}
        </div>
      ))}
    </div>
  );
}