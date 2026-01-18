package com.example.bbm;

import net.rim.device.api.ui.component.ButtonField;
import net.rim.device.api.ui.component.EditField;
import net.rim.device.api.ui.container.MainScreen;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.system.ApplicationPermissionsManager;
import net.rim.device.api.ui.component.Dialog;

public class LoginScreen extends MainScreen {
    private EditField emailField;
    private EditField passwordField;

    public LoginScreen() {
        setTitle(new LabelField("Chat (BB9780)"));
        add(new LabelField("Email:"));
        emailField = new EditField();
        add(emailField);
        add(new LabelField("Password:"));
        passwordField = new EditField("", "", 30, EditField.PASSWORD);
        add(passwordField);
        ButtonField loginBtn = new ButtonField("Login", ButtonField.CLICKABLE);
        add(loginBtn);
        loginBtn.setChangeListener(new FieldChangeListenerImpl());
    }

    private class FieldChangeListenerImpl implements net.rim.device.api.ui.FieldChangeListener {
        public void fieldChanged(Field field, int context) {
            final String email = emailField.getText();
            final String pass = passwordField.getText();
            // Basic validation
            if (email == null || email.length() == 0) {
                Dialog.alert("Enter email");
                return;
            }
            // Network call on background thread
            new Thread() {
                public void run() {
                    try {
                        String server = "http://YOUR_SERVER_HOST:3000";
                        String body = "{\"email\":\"" + email + "\",\"password\":\"" + pass + "\"}";
                        String res = HttpHelper.post(server + "/api/login", body, null);
                        // expecting JSON like { token, userId, email }
                        // VERY simple parsing (in production use a JSON lib)
                        if (res.indexOf("token") >= 0) {
                            final String token = HttpHelper.simpleExtract(res, "token");
                            final String userId = HttpHelper.simpleExtract(res, "userId");
                            // store token locally (simple store)
                            LocalStoreHelper.store("token", token);
                            LocalStoreHelper.store("userId", userId);
                            // switch to chat screen
                            UiApplication.getUiApplication().invokeLater(new Runnable() {
                                public void run() {
                                    UiApplication.getUiApplication().pushScreen(new ConversationsScreen());
                                }
                            });
                        } else {
                            UiApplication.getUiApplication().invokeLater(new Runnable() {
                                public void run() {
                                    Dialog.alert("Login failed");
                                }
                            });
                        }
                    } catch (final Exception e) {
                        UiApplication.getUiApplication().invokeLater(new Runnable() {
                            public void run() {
                                Dialog.alert("Network error: " + e.getMessage());
                            }
                        });
                    }
                }
            }.start();
        }
    }
}