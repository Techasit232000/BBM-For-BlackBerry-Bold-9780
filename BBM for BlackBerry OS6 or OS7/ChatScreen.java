package com.example.bbm;

import net.rim.device.api.ui.container.MainScreen;
import net.rim.device.api.ui.component.EditField;
import net.rim.device.api.ui.component.ButtonField;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.component.RichTextField;
import net.rim.device.api.ui.UiApplication;

public class ChatScreen extends MainScreen {
    private String otherUserId;
    private RichTextField messageArea;
    private EditField input;

    public ChatScreen(String otherUserId) {
        this.otherUserId = otherUserId;
        setTitle(new LabelField("Chat with " + otherUserId));
        messageArea = new RichTextField("", RichTextField.TEXT_WRAP);
        add(messageArea);
        input = new EditField();
        add(input);
        ButtonField send = new ButtonField("Send");
        add(send);
        send.setChangeListener(new FieldChangeListenerImpl());
        // Start polling thread
        new Thread(new PollingRunnable()).start();
    }

    private class FieldChangeListenerImpl implements net.rim.device.api.ui.FieldChangeListener {
        public void fieldChanged(Field field, int context) {
            final String txt = input.getText();
            if (txt == null || txt.length() == 0) return;
            input.setText("");
            new Thread() {
                public void run() {
                    try {
                        String server = "http://YOUR_SERVER_HOST:3000";
                        String token = LocalStoreHelper.load("token");
                        String body = "{\"toUserId\":\"" + otherUserId + "\",\"body\":\"" + HttpHelper.escapeBody(txt) + "\"}";
                        String res = HttpHelper.post(server + "/api/send", body, token);
                        // append local echo
                        UiApplication.getUiApplication().invokeLater(new Runnable() {
                            public void run() {
                                messageArea.setText(messageArea.getText() + "\nMe: " + txt);
                            }
                        });
                    }
