package io.advantageous.qbit.http.jetty.test;

import io.advantageous.qbit.http.client.HttpClient;
import io.advantageous.qbit.http.client.HttpClientBuilder;
import io.advantageous.qbit.http.request.HttpRequestBuilder;
import io.advantageous.qbit.http.server.HttpServer;
import io.advantageous.qbit.http.server.HttpServerBuilder;
import io.advantageous.qbit.http.websocket.WebSocket;
import io.advantageous.qbit.http.server.websocket.WebSocketMessage;
import io.advantageous.qbit.http.server.websocket.WebSocketMessageBuilder;
import org.boon.core.Sys;
import org.junit.Test;

import java.util.function.Consumer;

import static org.boon.Boon.puts;
import static org.boon.Exceptions.die;

/**
 * Created by rhightower on 2/15/15.
 */
public class HttpClientServerJettyTest {
    volatile boolean requestReceived;
    volatile boolean responseReceived;
    HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
    WebSocketMessageBuilder webSocketMessageBuilder = new WebSocketMessageBuilder();
    HttpClient client;
    HttpServer server;

    public void connect(int port) {

        client = new HttpClientBuilder().setPort(port).build();

        server = new HttpServerBuilder().setPort(port).build();

        requestReceived = false;
        responseReceived = false;

    }


    @Test
    public void testWebSocket() {

        connect(9090);


        server.setWebSocketMessageConsumer(webSocketMessage -> {

            puts(webSocketMessage.address(), webSocketMessage.body());
            if (webSocketMessage.getMessage().equals("What do you want on your cheeseburger?")) {
                webSocketMessage.getSender().sendText("Bacon");
                requestReceived = true;

            } else {
                puts("Websocket message", webSocketMessage.getMessage());
            }
        });


        run();


        final WebSocket webSocket = client.createWebSocket("/services/cheeseburger");

        webSocket.setTextMessageConsumer(message -> {
            if (message.equals("Bacon")) {
                responseReceived = true;
            }
        });


        webSocket.setOpenConsumer(aVoid -> webSocket.sendText("What do you want on your cheeseburger?"));

        webSocket.open();


        client.flush();

        Sys.sleep(1000);


        validate();
        stop();

    }


    @Test
    public void testNewOpenWaitWebSocket() {

        connect(9090);


        server.setWebSocketMessageConsumer(webSocketMessage -> {

            puts(webSocketMessage.address(), webSocketMessage.body());
            if (webSocketMessage.getMessage().equals("What do you want on your cheeseburger?")) {
                webSocketMessage.getSender().sendText("Bacon");
                requestReceived = true;

            } else {
                puts("Websocket message", webSocketMessage.getMessage());
            }
        });


        run();


        final WebSocket webSocket = client.createWebSocket("/services/cheeseburger");

        webSocket.setTextMessageConsumer(message -> {
            if (message.equals("Bacon")) {
                responseReceived = true;
            }
        });

        webSocket.openAndWait();

        webSocket.sendText("What do you want on your cheeseburger?");


        client.flush();

        Sys.sleep(1000);


        validate();
        stop();

    }

    @Test
    public void testNewOpenWaitWebSocketNewServerStuff() {

        connect(9090);


        server.setWebSocketOnOpenConsumer(webSocket -> webSocket.setTextMessageConsumer(message -> {
            if (message.equals("What do you want on your cheeseburger?")) {
                webSocket.sendText("Bacon");
                requestReceived = true;
            } else {
                puts("Websocket message", message);
            }
        }));

        run();


        final WebSocket webSocket = client.createWebSocket("/services/cheeseburger");

        webSocket.setTextMessageConsumer(message -> {
            if (message.equals("Bacon")) {
                responseReceived = true;
            }
        });

        webSocket.openAndWait();

        webSocket.sendText("What do you want on your cheeseburger?");


        client.flush();

        Sys.sleep(1000);


        validate();
        stop();

    }

    @Test
    public void testHttpServerClient() throws Exception {


        connect(9191);


        server.setHttpRequestConsumer(request -> {
            requestReceived = true;
            puts("SERVER", request.getUri(), request.getBody());
            request.getResponse().response(200, "application/json", "\"ok\"");
        });

        run();

        requestBuilder.setRemoteAddress("localhost").setMethod("GET").setUri("/client/foo");

        requestBuilder.setTextResponse((code, mimeType, body) -> {
            responseReceived = true;

            puts("CLIENT", code, mimeType, body);

        });

        client.sendHttpRequest(requestBuilder.build());
        client.flush();

        validate();
        stop();
    }


    public void run() {

        server.start();
        client.start();
        Sys.sleep(500);
    }


    private void stop() {


        client.stop();
        server.stop();

        Sys.sleep(500);
    }

    public void validate() {

        Sys.sleep(500);


        if (!requestReceived) {
            die("Request not received");
        }


        if (!responseReceived) {
            die("Response not received");
        }

    }
}