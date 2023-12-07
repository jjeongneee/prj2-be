package com.example.prj2be.service;

import com.example.prj2be.AllSongDTO;
import com.example.prj2be.domain.Song;
import com.example.prj2be.mapper.CommentMapper;
import com.example.prj2be.mapper.FileMapper;
import com.example.prj2be.mapper.SongMapper;
import com.example.prj2be.util.Parse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class SongService {

    private final SongMapper songMapper;
    private final CommentMapper commentMapper;
    private final FileMapper fileMapper;
    private final S3Client s3;

    @Value("${image.file.prefix}")
    private String urlPrefix;

    @Value("${aws.s3.bucket.name}")
    private String bucket;

    public Boolean insertSong(Song song) throws IOException {

        // 한글 코드 파싱해서 저장
        song.setArtistHangulCode(Parse.hangulCode(song.getArtistName()));
        song.setTitleHangulCode(Parse.hangulCode(song.getTitle()));
        song.setLyricHangulCode(Parse.hangulCode(song.getLyric()));

        songMapper.insertSongPoint(song);
        songMapper.updateSongRequest(song);

        // artistCode 찾기 위함
        if (song.getArtistGroup().isBlank()) song.setArtistGroup("solo");

        // 자동완성 위한 전역에 새로 저장한 song 추가
        AllSongDTO.getSongList().add(song);

        Integer artistCode = songMapper.getArtistCode(song);

        return songMapper.insertSong(song, artistCode) == 1;

    }



    // 파일 업로드  ↓  ↓  ↓  ↓  ↓  ↓  ↓  ↓  ↓  ↓  ↓  ↓
    private void upload(Integer id, MultipartFile file) throws IOException {

        String key = "prj2/artist/" + id +  "/" + file.getOriginalFilename();

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .acl(ObjectCannedACL.PUBLIC_READ)
                .build();

        s3.putObject(objectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

    }




    // 기존에 있던거... 그냥 이걸 쓰면 되는건지....?
    //  -> 기존 수완이가 작성한 코드에 fileName만 추가하면 되는거였음..
    public void insertArtist(Song song, MultipartFile files) throws IOException {
        if (files == null) {
            songMapper.insertArtist(song, "");
        } else {
            songMapper.insertArtist(song, files.getOriginalFilename());
            upload(song.getArtistId(), files);
        }
    }



    public Integer getArtistCode(Song song) {
        return songMapper.getArtistCode(song);
    }


    public List<Song> getSongLimit100() {
        List<Song> songList = songMapper.getSongLimit100();

        for (int i = 0; i < songList.size(); i++) {
            songList.get(i).setIndexForPlay(i);
        }

        return songList;
    }

    public List<Map<String, Object>> getMood() {
        return songMapper.getMood();
    }

    public List<Map<String, Object>> getGenre() {
        return songMapper.getGenre();
    }

    public List<Song> getByFilter(List<String> genreList, List<String> moodList) {
        genreList = genreList.stream().filter(a -> !a.isEmpty()).map(a -> "%" + a + "%").toList();
        moodList = moodList.stream().filter(a -> !a.isEmpty()).map(a -> "%" + a + "%").toList();

        return songMapper.getByFilter(genreList, moodList);
    }

    // 필터 추가하여 검색
    public List<Song> getByCategoryAndKeyword(String category, String keyword, List<String> genreList, List<String> moodList) {
        genreList = genreList.stream().filter(a -> !a.isEmpty()).map(a -> "%" + a + "%").toList();
        moodList = moodList.stream().filter(a -> !a.isEmpty()).map(a -> "%" + a + "%").toList();

        return songMapper.getByCategoryAndKeyword(category, "%" + keyword + "%", genreList, moodList);
    }

    // 비슷한 곡 랜덤으로 5개 추출
    public List<Song> getByGenreAndMood(String genre, String mood, Integer id) {
        genre = "%" + genre + "%";
        mood = "%" + mood + "%";
        List<Song> list = songMapper.getByGenreAndMood(genre, mood, id);
        List<Song> newList = new ArrayList<>();
        List<Integer> numberList = new ArrayList<>();

        while (numberList.size() < Math.min(list.size(), 5)) {
            int number = (int) (Math.random() * list.size());
            if (!numberList.contains(number)) numberList.add(number);
        }

        for (int i : numberList) {
            newList.add(list.get(i));
        }

        return newList;
    }

    public Integer getCode(String category, Song s) {
        if (category.equals("가수")) return s.getArtistHangulCode();
        else if (category.equals("제목")) return s.getTitleHangulCode();
        else return s.getLyricHangulCode();
    }

    public String getByCategory(String category, Song s) {
        if (category.equals("가수")) return s.getArtistName();
        else if (category.equals("제목")) return s.getTitle();
        else return s.getLyric();
    }

    public List<Song> autoComplete(String keyword, String category) {

        List<Song> songList = AllSongDTO.getSongList();

        switch (keyword) {
            case "ㄱ" -> {
                return songList.stream().filter(a -> getCode(category, a) == 1).toList();
            }
            case "ㄴ" -> {
                return songList.stream().filter(a -> getCode(category, a) == 2).toList();
            }
            case "ㄷ" -> {
                return songList.stream().filter(a -> getCode(category, a) == 3).toList();
            }
            case "ㄹ" -> {
                return songList.stream().filter(a -> getCode(category, a) == 4).toList();
            }
            case "ㅁ" -> {
                return songList.stream().filter(a -> getCode(category, a) == 5).toList();
            }
            case "ㅂ" -> {
                return songList.stream().filter(a -> getCode(category, a) == 6).toList();
            }
            case "ㅅ" -> {
                return songList.stream().filter(a -> getCode(category, a) == 7).toList();
            }
            case "ㅇ" -> {
                return songList.stream().filter(a -> getCode(category, a) == 8).toList();
            }
            case "ㅈ" -> {
                return songList.stream().filter(a -> getCode(category, a) == 9).toList();
            }
            case "ㅊ" -> {
                return songList.stream().filter(a -> getCode(category, a) == 10).toList();
            }
            case "ㅋ" -> {
                return songList.stream().filter(a -> getCode(category, a) == 11).toList();
            }
            case "ㅌ" -> {
                return songList.stream().filter(a -> getCode(category, a) == 12).toList();
            }
            case "ㅍ" -> {
                return songList.stream().filter(a -> getCode(category, a) == 13).toList();
            }
            case "ㅎ" -> {
                return songList.stream().filter(a -> getCode(category, a) == 14).toList();
            }
        }
        ;

        return songList.stream().filter(a -> getByCategory(category, a).contains(keyword)).toList();
    }

    public boolean insertRequest(Map<String, String> request) {

        if (request.get("title").isBlank() || request.get("artist").isBlank()) return false;

        return songMapper.insertRequest(request) == 1;
    }

    public Boolean isExist(String title, String artist) {
        List<Song> songList = AllSongDTO.getSongList();

        return songList.stream().filter(a -> a.getArtistName().equalsIgnoreCase(artist) && a.getTitle().equalsIgnoreCase(title)).count() >= 1;
    }

    public List<Map<String, Object>> requestList() {
        return songMapper.getByRequestList();
    }

    public Song getSongById(Integer id) {
        // /prj2/artist/50/카라.jpeg
        Song song = songMapper.getSongById(id);
        System.out.println("song = " + song);
        song.setArtistFileUrl(urlPrefix + "prj2/artist/" + song.getArtistId() + "/" + song.getArtistFileUrl());
        return song;
    }


    public boolean updateSongPointById(Integer songId) {
        Song song = songMapper.getSongById(songId);

        return songMapper.updateSongPoint(song) >= 1;
    }

    public List<Song> chartlist() {
        return songMapper.chartlist();
    }




    public boolean deleteMember(String id) {
        // 멤버가 작성한 댓글 삭제
        commentMapper.deleteByMemberId(id);

//    // songPage에 달린 댓글들 지우기
//    commentMapper.deleteBySongId(id);

        return songMapper.deleteById(id) == 1;
    }

    public List<Map<String, Object>> albumList(String album) {
        List<Map<String, Object>> albumList = songMapper.getByAlbumList(album);
        for (int i = 0; i < albumList.size(); i++) {
            albumList.get(i).put("id", i + 1);
        }

        return albumList;
    }

}
