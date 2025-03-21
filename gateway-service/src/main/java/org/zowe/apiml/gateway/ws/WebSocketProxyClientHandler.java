/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.ws;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.websocket.api.CloseException;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

/**
 * Copies data from the client to the server session.
 */
@Slf4j
public class WebSocketProxyClientHandler extends AbstractWebSocketHandler {
    private final WebSocketSession webSocketServerSession;

    public WebSocketProxyClientHandler(WebSocketSession webSocketServerSession) {
        this.webSocketServerSession = webSocketServerSession;
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> webSocketMessage) throws Exception {
        log.debug("handleMessage(session={},message={})", session, webSocketMessage);
        webSocketServerSession.sendMessage(webSocketMessage);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.debug("afterConnectionClosed(session={},status={})", session, status);
        webSocketServerSession.close(status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.warn("WebSocket transport error in session {}: {}", session.getId(), exception.getMessage());
        if (exception instanceof CloseException && exception.getCause() instanceof TimeoutException) {
            // Idle timeout
            webSocketServerSession.close(CloseStatus.NORMAL);
        } else if (exception instanceof CloseException) {
            webSocketServerSession.close(new CloseStatus(((CloseException) exception).getStatusCode(), exception.getMessage()));
        }
        super.handleTransportError(session, exception);
    }
}
