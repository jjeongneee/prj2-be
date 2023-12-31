package com.example.prj2be.domain;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class MyPlaylist {
    private String id;
    private String listId;
    private String memberId;
    private String listName;
    private Boolean isLike;
    private Integer songId;
    private Integer songs;
    private Integer countLike;
    private Integer totalSongCount;
    private String nickName;
    private Integer myplaylistcount;
    private LocalDate inserted;
    private String photo;
    private Boolean isSongContain;
    private String coverimage;
    private Boolean isLock;
}