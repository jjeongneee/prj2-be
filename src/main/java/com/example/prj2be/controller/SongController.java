package com.example.prj2be.controller;

import com.example.prj2be.AllSongDTO;
import com.example.prj2be.domain.Member;
import com.example.prj2be.domain.Song;
import com.example.prj2be.service.SongService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/song")
public class SongController {

  private final SongService songService;

  //  top100 찾기
  @GetMapping("top100")
  public List<Song> top100() {
    return songService.getSongLimit100();
  }

  //  모든 mood 찾기
  @GetMapping("mood")
  public List<Map<String, Object>> mood() {
    return songService.getMood();
  }

  //  모든 genre 찾기
  @GetMapping("genre")
  public List<Map<String, Object>> genre() {
    return songService.getGenre();
  }

  //  필터 거친 top100 찾기
  @GetMapping("ft100")
  public List<Song> filter(@RequestParam(value = "genre", required = false) List<String> genreList,
                           @RequestParam(value = "mood", required = false) List<String> moodList
  ) {

    return songService.getByFilter(genreList, moodList);
  }

  //  검색
  @GetMapping("search")
  public List<Song> search(@RequestParam("sc") String category,
                           @RequestParam("sk") String keyword,
                           @RequestParam(value = "genre", required = false) List<String> genreList,
                           @RequestParam(value = "mood", required = false) List<String> moodList) {
    return songService.getByCategoryAndKeyword(category, keyword, genreList, moodList);
  }

  //  비슷한 곡 탐색
  @GetMapping("similar")
  public List<Song> similar(String genre, String mood, Integer id) {
    return songService.getByGenreAndMood(genre, mood, id);
  }

  // 자동 완성
  @GetMapping("autoComplete")
  public List<Song> autoComplete(@RequestParam("sc") String category,
                                 @RequestParam("sk") String keyword) {

    return songService.autoComplete(keyword, category);
  }

  // 요청 받기 전에 그 곡이 db에 있는지 확인
  @GetMapping("isExist")
  public Boolean isExist(String title, String artist) {
    return songService.isExist(title, artist);
  }

  // 요청 받은 곡 insert
  @PostMapping("request")
  public ResponseEntity request(@RequestBody Map<String, String> request,
                                @SessionAttribute(value = "login", required = false) Member login) {

    if (login == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

    if (songService.insertRequest(request)) return ResponseEntity.ok().build();
    else return ResponseEntity.badRequest().build();
  }
}
