package com.DevLib.util;

import jakarta.mail.Message;
import jakarta.mail.internet.MimeMultipart;

public class ImapUtil {
    // 메일 본문이 텍스트일 수도, 복합(Multipart)일 수도 있어서 처리하는 유틸리티
    public static String getTextFromMessage(Message message) throws Exception {
        if (message.isMimeType("text/plain")) {
            return message.getContent().toString();
        } else if (message.isMimeType("multipart/*")) {
            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
            return mimeMultipart.getBodyPart(0).getContent().toString();
        }
        return "";
    }
}
