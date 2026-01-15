package kr.co.ultari.at_board.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
public class AtBoardController {
    @RequestMapping("/{key}")
    public String index(Model model, @PathVariable("key") String userId) {
        log.info(userId);
        model.addAttribute("userId", userId);
        return "index";
    }
}
