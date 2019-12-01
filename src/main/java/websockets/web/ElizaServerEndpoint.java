package websockets.web;

import org.glassfish.grizzly.Grizzly;
import websockets.service.Eliza;

import javax.websocket.*;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.HashMap;

@ServerEndpoint(value = "/eliza")
public class ElizaServerEndpoint {

  private static final Logger LOGGER = Grizzly.logger(ElizaServerEndpoint.class);
  private Eliza eliza = new Eliza();
  static private HashMap<Session, String> sessions = new HashMap<Session, String>();
  
  private synchronized void add(Session session) {
    sessions.put(session, null);
  }
  
  private synchronized void setName(Session session, String name) {
    sessions.put(session, name);
  }
  
  private synchronized void del(Session session) {
    sessions.remove(session);
  }

  @OnOpen
  public void onOpen(Session session) {
    LOGGER.info("Server Connected ... " + session.getId());
    session.getAsyncRemote().sendText("The doctor, you and " + sessions.size() + " more are in.");
    add(session);
    session.getAsyncRemote().sendText("DOCTOR: What's your name?");
    session.getAsyncRemote().sendText("---");
    
  }

  @OnMessage
  public void onMessage(String message, Session session) throws IOException {
    LOGGER.info("Server Message ... " + session.getId());
    Scanner currentLine = new Scanner(message.toLowerCase());
    if (sessions.get(session) == null) {
      setName(session, message.split(" ", 2)[0].toUpperCase());
      for (Session s : sessions.keySet()) {
        if (!s.equals(session)) {          
          s.getAsyncRemote().sendText(sessions.get(session) + " joined the chat.");
        }
      }
    } else {
      LOGGER.info("Server recieved \"" + message + "\"");
      String name = sessions.get(session);
      for (Session s : sessions.keySet()) {
        if (!s.equals(session)) { 
          s.getAsyncRemote().sendText(name + ": " + message);
        }
      }
      String response;
      boolean isBye = currentLine.findInLine("bye") != null;
      if (!isBye) {
        response = eliza.respond(currentLine);
      } else {
        response = "Alright then, goodbye!";
      }
      for (Session s : sessions.keySet()) {
        s.getAsyncRemote().sendText("DOCTOR: " + name + ", " + response);
        session.getAsyncRemote().sendText("---");
      }
      if (isBye) {
        session.close(new CloseReason(CloseCodes.NORMAL_CLOSURE, "Alright then, goodbye!"));
      }
    }
  }

  @OnClose
  public void onClose(Session session, CloseReason closeReason) {
    LOGGER.info(String.format("Session %s closed because of %s", session.getId(), closeReason));
    String name = sessions.get(session);
    del(session);
    if (name != null) {
      for (Session s: sessions.keySet()) {
        s.getAsyncRemote().sendText(name + " left the chat.");
      }
    }
  }

  @OnError
  public void onError(Session session, Throwable errorReason) {
    LOGGER.log(Level.SEVERE,
            String.format("Session %s closed because of %s", session.getId(), errorReason.getClass().getName()),
            errorReason);
  }

}
