package com.studycoAchl.hackaton.DTO;

public class MessageRequest {
    private String sender;    // "USER" 또는 "AI"
    private String content;   // 메시지 내용
    private String imageUrl;  // 이미지 URL (선택사항)

    // 기본 생성자
    public MessageRequest() {}

    // Getters and Setters
    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}