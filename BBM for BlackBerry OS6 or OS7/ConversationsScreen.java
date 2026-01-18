package com.example.bbm;

import net.rim.device.api.ui.container.MainScreen;
import net.rim.device.api.ui.component.ButtonField;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.Field;

public class ConversationsScreen extends MainScreen {
    public ConversationsScreen() {
        setTitle(new LabelField("Conversations"));
        // For MVP we show a single input to chat with another user ID
        add(new LabelField("Enter recipient userId:"));
        final com.example.bbm.SimpleEditField other = new com.example.bbm.SimpleEditField();
        add(other);
        ButtonField open = new ButtonField("Open Chat");
        add(open);
        open.setChangeListener(new FieldChangeListenerImpl(other));
    }

    private class FieldChangeListenerImpl implements net.rim.device.api.ui.FieldChangeListener {
        private com.example.bbm.SimpleEditField other;
        public FieldChangeListenerImpl(com.example.bbm.SimpleEditField other) { this.other = other; }
        public void fieldChanged(Field field, int context) {
            UiApplication.getUiApplication().pushScreen(new ChatScreen(other.getText()));
        }
    }
}