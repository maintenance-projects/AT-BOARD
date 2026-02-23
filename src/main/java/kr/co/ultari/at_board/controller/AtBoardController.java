package kr.co.ultari.at_board.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
public class AtBoardController {
    @RequestMapping("/")
    public String home() {
        return "redirect:/board";
    }

    @GetMapping("/favicon.ico")
    public ResponseEntity<Void> favicon() {
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @RequestMapping("/{key}")
    public String index(Model model, @PathVariable("key") String userId) {
        log.info(userId);
        model.addAttribute("userId", userId);
        return "index";
    }
}
