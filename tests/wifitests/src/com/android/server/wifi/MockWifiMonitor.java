/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.wifi;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import android.os.Handler;
import android.os.Message;
import android.util.SparseArray;

import com.android.server.wifi.MockAnswerUtil.AnswerWithArguments;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Creates a WifiMonitor and installs it as the current WifiMonitor instance
 * WARNING: This does not perfectly mock the behavior of WifiMonitor at the moment
 *          ex. startMoniroting does nothing and will not send a connection/disconnection event
 */
public class MockWifiMonitor {
    private final WifiMonitor mWifiMonitor;

    public MockWifiMonitor() throws Exception {
        mWifiMonitor = mock(WifiMonitor.class);

        Field field = WifiMonitor.class.getDeclaredField("sWifiMonitor");
        field.setAccessible(true);
        field.set(null, mWifiMonitor);

        doAnswer(new RegisterHandlerAnswer())
                .when(mWifiMonitor).registerHandler(anyString(), anyInt(), any(Handler.class));

    }

    private final Map<String, SparseArray<Handler>> mHandlerMap = new HashMap<>();
    private class RegisterHandlerAnswer extends AnswerWithArguments<Void> {
        public void answer(String iface, int what, Handler handler) {
            SparseArray<Handler> ifaceHandlers = mHandlerMap.get(iface);
            if (ifaceHandlers == null) {
                ifaceHandlers = new SparseArray<>();
                mHandlerMap.put(iface, ifaceHandlers);
            }
            ifaceHandlers.put(what, handler);
        }
    }

    /**
     * Send a message and assert that it was dispatched to a handler
     */
    public void sendMessage(String iface, int what) {
        sendMessage(iface, Message.obtain(null, what));
    }
    public void sendMessage(String iface, Message message) {
        SparseArray<Handler> ifaceHandlers = mHandlerMap.get(iface);
        if (ifaceHandlers != null) {
            assertTrue("No handler for iface=" + iface + ",what=" + message.what,
                    sendMessage(ifaceHandlers, message));
        } else {
            boolean sent = false;
            for (Map.Entry<String, SparseArray<Handler>> entry : mHandlerMap.entrySet()) {
                if (sendMessage(entry.getValue(), Message.obtain(message))) {
                    sent = true;
                }
            }
            assertTrue("No handler for message with nonexistant iface, iface=" + iface
                    + ",what=" + message.what, sent);
        }
    }
    private boolean sendMessage(SparseArray<Handler> ifaceHandlers, Message message) {
        Handler handler = ifaceHandlers.get(message.what);
        if (handler != null) {
            message.setTarget(handler);
            message.sendToTarget();
            return true;
        }
        return false;
    }

}
