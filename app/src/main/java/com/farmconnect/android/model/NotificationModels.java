package com.farmconnect.android.model;

import java.util.List;

public class NotificationModels {
    public static class NotificationResponse {
        public long notificationId;
        public String title;
        public String message;
        public String type;
        public boolean isRead;
        public String createdAt;
    }
}
