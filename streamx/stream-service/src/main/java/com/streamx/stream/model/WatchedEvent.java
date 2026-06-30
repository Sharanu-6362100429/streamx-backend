package com.streamx.stream.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchedEvent {
    private String userId;
    private String videoKey;
    private long watchedAt;
}