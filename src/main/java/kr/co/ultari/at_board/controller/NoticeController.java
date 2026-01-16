package kr.co.ultari.at_board.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class NoticeController {

    @GetMapping({"/notice"})
    public String recentNotices() {
        return "pages/notice/list";
    }
}